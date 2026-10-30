import{test,expect}from'@playwright/test';
import{fileURLToPath}from'node:url';
test('capture the seeded operations workspace',async({page})=>{
 await page.goto('/');
 await page.getByLabel('Password',{exact:true}).fill('local-demo-password');
 await page.getByRole('button',{name:'Open workspace',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Keep commerce moving.'})).toBeVisible();
 await expect(page.getByRole('button',{name:/Elm Office/})).toBeVisible();
 await page.screenshot({path:fileURLToPath(new URL('../../docs/orderflow-preview.png',import.meta.url)),fullPage:true});
});
