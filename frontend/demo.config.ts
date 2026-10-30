import{defineConfig}from'@playwright/test';
export default defineConfig({testDir:'./demo-tests',use:{baseURL:'http://127.0.0.1:3202'},webServer:{command:"PORT=3202 PUBLIC_DEMO=true JDBC_DATABASE_URL='jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' SEED_DEMO=true ../scripts/start.sh",url:'http://127.0.0.1:3202/api/health',timeout:120000,reuseExistingServer:false},reporter:'list'});
