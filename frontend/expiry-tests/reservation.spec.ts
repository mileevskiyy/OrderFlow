import{test,expect}from'@playwright/test';
test('unpaid reservation expires automatically, releases stock and retains late-payment review',async({page})=>{
 await page.goto('/',{waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'Try the demo',exact:true}).click();await expect(page.getByRole('button',{name:'Orders',exact:true})).toBeVisible();
 const headers={'X-CSRF-TOKEN':(await(await page.request.get('/api/session')).json()).csrfToken};
 async function post(path:string,data:unknown,key?:string){const r=await page.request.post('/api'+path,{maxRetries:key||path==='/payments/simulate'?2:0,headers:{...headers,...(key?{'Idempotency-Key':key}:{})},data});expect(r.ok(),await r.text()).toBe(true);return r.json();}
 const suffix=Date.now();const warehouse=await post('/warehouses',{name:`Expiry test ${suffix}`,location:'Hong Kong'}),product=await post('/products',{sku:`EXP-${suffix}`,name:`Expiry product ${suffix}`,category:'Test',priceCents:100});
 await post('/inventory/receive',{warehouseId:warehouse.id,productId:product.id,quantity:3,reason:'Synthetic expiry verification'},`expiry-stock-${suffix}`);
 const order=await post('/orders',{customer:`Expiry buyer ${suffix}`,warehouseId:warehouse.id,lines:[{productId:product.id,quantity:2}]},`expiry-order-${suffix}`);
 await page.reload({waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'Orders',exact:true}).click();await page.getByRole('button',{name:new RegExp(order.reference)}).first().click();
 await expect(page.getByRole('dialog')).toContainText(/Payment deadline:|Reservation expired/);
 await expect(page.getByText('Reservation expired',{exact:true})).toBeVisible({timeout:100000});
 await expect(page.getByRole('button',{name:'Simulate payment',exact:true})).toHaveCount(0);
 const stocks=await(await page.request.get('/api/inventory')).json(),position=stocks.find((s:any)=>s.product_id===product.id&&s.warehouse_id===warehouse.id);expect(position.on_hand).toBe(3);expect(position.reserved).toBe(0);expect(position.available).toBe(3);
 const result=await post('/payments/simulate',{eventId:`late-event-${suffix}`,orderId:order.id,amountCents:200,outcome:'SUCCEEDED'});expect(result.event.effect).toBe('LATE_SUCCESS_REVIEW');expect(result.order.status).toBe('CANCELLED');
 await page.reload({waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'Orders',exact:true}).click();await page.getByRole('combobox',{name:'Filter order status'}).selectOption('review');await page.getByRole('button',{name:new RegExp(order.reference)}).first().click();
 await expect(page.getByRole('dialog')).toContainText('Review is required; the order was not restored');
 if(!process.env.DEMO_URL){await page.screenshot({path:'../docs/reservation-expired.png'});await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:'../docs/reservation-expired-mobile.png'});}
});
