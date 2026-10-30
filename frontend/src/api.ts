import type {Session} from './types';
let csrf='';
export async function api<T>(path:string,method='GET',body?:unknown,key?:string):Promise<T>{
 const response=await fetch('/api'+path,{method,headers:{...(body?{'Content-Type':'application/json'}:{}),...(csrf?{'X-CSRF-TOKEN':csrf}:{}),...(key?{'Idempotency-Key':key}:{})},body:body?JSON.stringify(body):undefined});
 if(response.status===204)return undefined as T;
 const data=await response.json();if(!response.ok)throw new Error(data.error||`Request failed (${response.status})`);return data;
}
export async function session(){const value=await api<Session>('/session');csrf=value.csrfToken;return value;}
export async function login(username:string,password:string){await session();const response=await fetch('/api/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-CSRF-TOKEN':csrf},body:new URLSearchParams({username,password})});if(!response.ok)throw new Error('Incorrect username or password.');return session();}
export async function logout(){await api('/logout','POST');return session();}
export const key=()=>crypto.randomUUID();
