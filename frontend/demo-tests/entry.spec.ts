import{test,expect}from'@playwright/test';
test('visitor enters the seeded demo without credentials',async({page})=>{
 await page.goto('/');
 await page.getByRole('button',{name:'Try the demo',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Keep commerce moving.'})).toBeVisible();
 await expect(page.getByRole('button',{name:/Elm Office/})).toBeVisible();
 await page.reload();
 await expect(page.getByRole('heading',{name:'Keep commerce moving.'})).toBeVisible();
});
