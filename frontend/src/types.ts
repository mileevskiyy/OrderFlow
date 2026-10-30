export type OrderStatus='RESERVED'|'PAID'|'FULFILLED'|'PARTIALLY_RETURNED'|'RETURNED'|'CANCELLED';
export interface Product{id:string;sku:string;name:string;category:string;price_cents:number}
export interface Warehouse{id:string;name:string;location:string}
export interface Stock extends Product{product_id:string;warehouse_id:string;warehouse_name:string;on_hand:number;reserved:number;available:number}
export interface Order{id:string;reference:string;customer:string;warehouse_id:string;warehouse_name:string;status:OrderStatus;total_cents:number;refunded_cents:number;created_at:string;updated_at:string;units:number}
export interface OrderLine{id:string;product_id:string;name:string;sku:string;unit_price_cents:number;quantity:number;returned:number}
export interface Payment{event_id:string;outcome:string;amount_cents:number;effect:string;created_at:string}
export interface Activity{id:string;reference:string|null;order_id:string|null;action:string;detail:string;created_at:string}
export interface OrderDetail extends Order{lines:OrderLine[];payments:Payment[];returns:{id:string;refund_cents:number;reason:string;created_at:string}[];timeline:Activity[]}
export interface Overview{orders:{total:number;awaiting_payment:number;ready_to_ship:number;net_paid_cents:number;refunded_cents:number};stock:{on_hand:number;reserved:number;available:number;low_stock_positions:number};products:number}
export interface Session{authenticated:boolean;username:string;csrfToken:string;publicDemo:boolean}
export interface Movement{id:string;warehouse_name:string;name:string;sku:string;reference:string|null;kind:string;on_hand_delta:number;reserved_delta:number;reason:string;created_at:string}
