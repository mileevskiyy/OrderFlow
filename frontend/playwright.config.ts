import{defineConfig}from'@playwright/test';
export default defineConfig({testDir:'./tests',use:{baseURL:'http://127.0.0.1:3200',trace:'retain-on-failure'},webServer:{command:"JDBC_DATABASE_URL='jdbc:h2:mem:browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' SEED_DEMO=false ../scripts/start.sh",url:'http://127.0.0.1:3200/api/health',timeout:120000,reuseExistingServer:false},reporter:'html'});
