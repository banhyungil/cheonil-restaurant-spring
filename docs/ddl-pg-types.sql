create type public.order_status as enum ('READY', 'COOKED', 'PAID');

alter type public.order_status owner to root;

create type public.pay_type as enum ('CASH', 'CARD');

alter type public.pay_type owner to root;

create type public.day_type as enum ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');

alter type public.day_type owner to root;

create type public.rsv_status as enum ('RESERVED', 'COMPLETED', 'CANCELED');

alter type public.rsv_status owner to root;


