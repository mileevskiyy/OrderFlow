#!/usr/bin/env node
/** Reproducible local/CI exercise: real OrderFlow UI -> private QualityHub -> evidence comparison. */
import {spawn,execFileSync} from 'node:child_process';
import {mkdtemp,mkdir,readFile,writeFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createServer} from 'node:net';
import {randomBytes,createHash} from 'node:crypto';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const qualityhub=resolve(process.env.QUALITYHUB_CHECKOUT||resolve(root,'../qualityhub'));
assert.equal(await readFile(resolve(qualityhub,'integrations/playwright-reporter.ts'),'utf8'),await readFile(resolve(root,'frontend/integrations/qualityhub-reporter.ts'),'utf8'),'Vendored reporter must match the pinned QualityHub checkout.');
const evidence=resolve(root,'.qualityhub-evidence');await mkdir(evidence,{recursive:true});
const data=await mkdtemp(resolve(tmpdir(),'orderflow-qualityhub-'));
const socket=createServer();await new Promise(r=>socket.listen(0,'127.0.0.1',r));const port=socket.address().port;await new Promise(r=>socket.close(r));
const env={...process.env,NODE_ENV:'test',PUBLIC_DEMO:'false',SEED_DEMO:'false',DATA_DIR:data,HOST:'127.0.0.1',PORT:String(port)};
delete env.DATABASE_URL;delete env.SETUP_KEY;
const server=spawn(process.execPath,['--import','tsx','server/index.ts'],{cwd:qualityhub,env,stdio:['ignore','pipe','pipe']});
let output='';for(const stream of [server.stdout,server.stderr])stream.on('data',b=>{output=(output+b.toString()).slice(-6000);});
const base=`http://127.0.0.1:${port}`;let cookie='',csrf='';
async function request(path,method='GET',body){
 const r=await fetch(base+'/api'+path,{method,headers:{...(body?{'Content-Type':'application/json'}:{}),...(cookie?{Cookie:cookie}:{}),...(csrf?{'X-CSRF-Token':csrf}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)});
 if(!r.ok)throw new Error(`QualityHub ${method} ${path}: HTTP ${r.status}`);return r;
}
const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
async function playwright(name,scenario,token,project){
 const path=resolve(evidence,name+'.json');
 const buildUrl=process.env.GITHUB_RUN_ID&&process.env.GITHUB_REPOSITORY?`https://github.com/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`:undefined;
 const runEnv={...process.env,QUALITYHUB_URL:base,QUALITYHUB_TOKEN:token,QUALITYHUB_PROJECT_ID:project,QUALITYHUB_RUN_ID:`${process.env.GITHUB_RUN_ID||Date.now()}:${process.env.GITHUB_RUN_ATTEMPT||'local'}:${name}`,QUALITYHUB_RUN_NAME:`OrderFlow · ${name}`,QUALITYHUB_REPORT_PATH:path,QUALITYHUB_REPOSITORY:'mileevskiyy/OrderFlow',QUALITYHUB_COMMIT:git('rev-parse','HEAD'),QUALITYHUB_DIRTY:String(Boolean(git('status','--porcelain','--untracked-files=no'))),QUALITYHUB_SCENARIO:scenario,QUALITYHUB_INJECT_ORDER_FAILURE:scenario==='injected-order-api-503'?'true':'false',...(buildUrl?{QUALITYHUB_BUILD_URL:buildUrl}:{})};
 const child=spawn('npm',['run','test:e2e'],{cwd:resolve(root,'frontend'),env:runEnv,stdio:'inherit'});
 const code=await new Promise((res,rej)=>{child.once('error',rej);child.once('exit',res);});
 assert.equal(code,scenario==='injected-order-api-503'?1:0,`Unexpected Playwright exit for ${scenario}`);
 const report=JSON.parse(await readFile(path,'utf8'));
 assert.ok(report.attachments.some(a=>a.kind==='screenshot'),'Expected real screenshot evidence');
 assert.ok(report.attachments.some(a=>a.kind==='trace'),'Expected real Playwright trace evidence');
 if(scenario==='injected-order-api-503')assert.ok(report.attachments.some(a=>a.kind==='log'&&Buffer.from(a.contentBase64,'base64').toString().includes('Order API 503 injected: true')),'The controlled request failure must actually have been injected.');
 const runs=await(await request(`/projects/${project}/runs`)).json();
 let found;
 for(const r of runs){const full=await(await request(`/projects/${project}/runs/${r.id}`)).json();if(full.automation?.externalId===report.externalId)found=full;}
 assert.ok(found,'Reporter did not deliver its run');assert.equal(found.status,'completed');assert.equal(found.automation.source.commit,runEnv.QUALITYHUB_COMMIT);assert.equal(found.automation.source.scenario,scenario);
 let files=0;
 for(const result of found.results){
  const metadata=await(await request(`/projects/${project}/results/${result.id}/attachments`)).json();
  for(const a of metadata){const bytes=Buffer.from(await(await request(`/projects/${project}/attachments/${a.id}`)).arrayBuffer());assert.equal(createHash('sha256').update(bytes).digest('hex'),a.sha256);files++;}
 }
 assert.equal(files,report.attachments.length,'Some report attachments were not retained');
 // Replay exactly the saved report and files through write-only project token endpoints.
 const send=async(path,body)=>{const r=await fetch(base+'/api/projects/'+project+path,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`},body:JSON.stringify(body),signal:AbortSignal.timeout(15000)});assert.ok(r.ok,`Replay failed: HTTP ${r.status}`);return r.json();};
 const replay=await send('/imports/playwright',{externalId:report.externalId,name:report.name,source:report.source,tests:report.tests});assert.equal(replay.replayed,true);
 for(const a of report.attachments)assert.equal((await send('/imports/playwright/attachments',{...a,runExternalId:report.externalId})).replayed,true);
 return {run:found,files};
}
try{
 let ready=false;for(let i=0;i<100;i++){if(server.exitCode!==null)throw new Error('QualityHub failed to start: '+output);try{const r=await fetch(base+'/api/health',{signal:AbortSignal.timeout(1000)});if(r.ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,300));}assert.ok(ready,'QualityHub readiness timeout');
 const registration=await request('/auth/register','POST',{name:'OrderFlow CI operator',email:'ci@example.test',password:randomBytes(32).toString('hex')});cookie=registration.headers.get('set-cookie').split(';')[0];csrf=(await(await request('/auth/me')).json()).csrf;
 const project=await(await request('/projects','POST',{name:'OrderFlow integration',description:'Real browser tests. The baseline injects an explicitly controlled API failure; recovery uses the normal app.'})).json();
 const token=await(await request(`/projects/${project.id}/tokens`,'POST',{name:'Ephemeral OrderFlow CI'})).json();
 const baseline=await playwright('controlled-failure','injected-order-api-503',token.token,project.id);assert.equal(baseline.run.results.filter(r=>r.status==='failed').length,1);
 const failed=baseline.run.results.find(r=>r.status==='failed');await request(`/projects/${project.id}/defects`,'POST',{resultId:failed.id,title:'Controlled OrderFlow order API outage',description:'Integration exercise: Playwright deliberately replaced POST /api/orders with HTTP 503. This is not a production incident.',severity:'high'});
 const candidate=await playwright('recovery','normal-order-workflow',token.token,project.id);assert.ok(candidate.run.results.every(r=>r.status==='passed'));
 const comparison=await(await request(`/projects/${project.id}/runs/compare?baseline=${baseline.run.id}&candidate=${candidate.run.id}`)).json();assert.equal(comparison.summary.fixed,1);
 const exportData=await(await request(`/projects/${project.id}/export`)).json();assert.equal(exportData.runs.length,2);assert.equal(exportData.defects.length,1);
 await writeFile(resolve(evidence,'workspace-export.json'),JSON.stringify(exportData,null,2));await writeFile(resolve(evidence,'comparison.json'),JSON.stringify(comparison,null,2));
 await writeFile(resolve(evidence,'summary.json'),JSON.stringify({experiment:'controlled API outage followed by normal OrderFlow workflow',orderflowCommit:git('rev-parse','HEAD'),orderflowWorkingTreeDirty:Boolean(git('status','--porcelain','--untracked-files=no')),qualityhubCommit:execFileSync('git',['rev-parse','HEAD'],{cwd:qualityhub,encoding:'utf8'}).trim(),runs:2,fixed:comparison.summary.fixed,verifiedFiles:baseline.files+candidate.files,deliveryReplayVerified:true},null,2));
 console.log(`OrderFlow → QualityHub verified: 2 runs, 1 controlled failure recovered, ${baseline.files+candidate.files} downloaded files matched SHA-256. Evidence: .qualityhub-evidence/`);
}finally{
 server.kill('SIGTERM');await Promise.race([new Promise(r=>server.once('exit',r)),new Promise(r=>setTimeout(r,3000))]);if(server.exitCode===null)server.kill('SIGKILL');await rm(data,{recursive:true,force:true});
}
