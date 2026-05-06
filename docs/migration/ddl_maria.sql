create table cheonil.Expense
(
seq bigint unsigned auto_increment comment '지출 SEQ'
primary key,
ctgSeq int unsigned not null comment '카테고리 SEQ',
storeSeq smallint unsigned null comment '매장 SEQ',
name varchar(50) not null comment '지출명',
amount int unsigned not null comment '금액',
expenseAt datetime not null comment '지출일',
cmt varchar(400) null comment '비고',
options longtext collate utf8mb4_bin null comment '추가 정보'
check (json_valid(`options`)),
updatedAt datetime default current_timestamp() null comment '수정시간'
)
comment '지출' collate = utf8mb4_general_ci;

create index FK_Expense_TO_ExpenseCategory
on cheonil.Expense (ctgSeq);

create index FK_Expense_TO_Store
on cheonil.Expense (storeSeq);

create table cheonil.ExpenseCategory
(
seq int unsigned auto_increment comment '지출 카테고리 SEQ'
primary key,
path varchar(50) not null comment '카테고리명',
options longtext collate utf8mb4_bin null comment '추가 정보'
check (json_valid(`options`))
)
comment '지출 카네고리' collate = utf8mb4_general_ci;

create table cheonil.ExpenseProduct
(
expsSeq bigint unsigned not null comment '지출 Seq',
prdSeq int unsigned not null comment '제품 SEQ',
cnt smallint unsigned not null comment '수량',
price bigint unsigned not null comment '가격',
unitCnt smallint unsigned null comment '단위수량',
cmt varchar(400) null comment '비고',
primary key (expsSeq, prdSeq)
)
comment '지출 제퓸' collate = utf8mb4_general_ci;

create index FK_ExpenseProduct_TO_Expense
on cheonil.ExpenseProduct (expsSeq);

create index FK_ExpenseProduct_TO_Product
on cheonil.ExpenseProduct (prdSeq);

create table cheonil.Menu
(
seq smallint unsigned auto_increment comment '메뉴 Seq'
primary key,
ctgSeq smallint unsigned not null comment '메뉴 카테고리 Seq',
name varchar(45) not null comment '메뉴 명',
abv varchar(10) null comment '이름 약어',
price int unsigned not null comment '가격',
cmt varchar(1000) null comment '비고',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간',
constraint name
unique (name)
)
comment '메뉴' collate = utf8mb4_general_ci;

create index FK_MenuCategory_TO_Menu
on cheonil.Menu (ctgSeq);

create table cheonil.MenuCategory
(
seq smallint unsigned auto_increment comment '메뉴 카테고리 Seq'
primary key,
name varchar(20) not null comment '메뉴 카테고리 명',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간',
constraint name
unique (name)
)
comment '메뉴 카테고리' collate = utf8mb4_general_ci;

create table cheonil.MyOrder
(
seq bigint unsigned auto_increment comment '주문 Seq'
primary key,
storeSeq smallint unsigned not null comment '매장 Seq',
amount int unsigned not null comment '총 금액',
status enum ('READY', 'COOKED', 'PAID') default 'READY' not null comment 'READY: 준비, COOKED: 조리 완료, PAID: 결제 완료',
orderAt datetime default current_timestamp() not null comment '주문 시간',
cookedAt datetime null comment '조리완료 시간',
cmt varchar(1000) null comment '비고',
updatedAt datetime default current_timestamp() not null comment '수정시간'
)
comment '주문' collate = utf8mb4_general_ci;

create index FK_Store_TO_MyOrder
on cheonil.MyOrder (storeSeq);

create table cheonil.OrderMenu
(
menuSeq smallint unsigned not null comment '메뉴 Seq',
orderSeq bigint unsigned not null comment '주문 Seq',
price int unsigned not null comment '가격 menu는 가격이 바뀔수가 있음',
cnt tinyint unsigned not null comment '수량',
primary key (menuSeq, orderSeq)
)
comment '주문 메뉴' collate = utf8mb4_general_ci;

create index FK_MyOrder_TO_OrderMenu
on cheonil.OrderMenu (orderSeq);

create table cheonil.OrderMenuRsv
(
menuSeq smallint unsigned not null comment '메뉴 Seq',
orderRsvSeq bigint unsigned not null comment '주문예약 Seq',
price int unsigned not null comment '가격 menu는 가격이 바뀔수가 있음',
cnt tinyint unsigned not null comment '수량',
primary key (menuSeq, orderRsvSeq)
)
comment '주문 메뉴 예약' collate = utf8mb4_general_ci;

create index FK_OrderRsv_TO_OrderMenuRsv
on cheonil.OrderMenuRsv (orderRsvSeq);

create table cheonil.OrderRsv
(
seq bigint unsigned auto_increment comment '주문예약 Seq'
primary key,
storeSeq smallint unsigned not null comment '매장 Seq',
amount int unsigned not null comment '총 금액',
rsvTime char(5) not null comment 'HH:MM',
dayType enum ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') null comment '요일',
cmt varchar(1000) null comment '비고',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간'
)
comment '주문 예약' collate = utf8mb4_general_ci;

create index FK_Store_TO_OrderRsv
on cheonil.OrderRsv (storeSeq);

create table cheonil.Payment
(
seq bigint unsigned auto_increment comment '결재 Seq'
primary key,
orderSeq bigint unsigned not null comment '주문 Seq',
amount int unsigned not null comment '결재 금액',
payType enum ('CASH', 'CARD') default 'CASH' not null comment 'CASH: 현금, CARD: 카드',
payAt datetime not null comment '지급날짜'
)
comment '결재' collate = utf8mb4_general_ci;

create index FK_MyOrder_TO_Payment
on cheonil.Payment (orderSeq);

create table cheonil.PlaceCategory
(
seq smallint unsigned auto_increment comment '장소 카테고리 Seq'
primary key,
name varchar(100) not null comment '장소 카테고리 명',
cmt varchar(1000) null comment '비고',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
constraint name
unique (name)
)
comment '장소 카테고리' collate = utf8mb4_general_ci;

create table cheonil.Product
(
seq int unsigned auto_increment comment 'SEQ'
primary key,
prdInfoSeq smallint unsigned not null comment '제품 SEQ',
unitSeq smallint unsigned not null comment '단위 SEQ',
unitCntList longtext collate utf8mb4_bin null comment '단위수량 목록'
check (json_valid(`unitCntList`))
)
comment '제품' collate = utf8mb4_general_ci;

create index FK_Product_TO_ProductInfo
on cheonil.Product (prdInfoSeq);

create index FK_Product_TO_Unit
on cheonil.Product (unitSeq);

create table cheonil.ProductInfo
(
seq smallint unsigned auto_increment comment '제품 Seq'
primary key,
suplSeq smallint unsigned not null comment '식자재 Seq',
name varchar(100) not null comment '식자재 명',
cmt varchar(200) null comment '비고',
options longtext collate utf8mb4_bin null comment '추가 정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간'
)
comment '제품 정보' collate = utf8mb4_general_ci;

create index FK_ProductInfo_TO_Supply
on cheonil.ProductInfo (suplSeq);

create table cheonil.Setting
(
seq smallint unsigned auto_increment comment '설정 Seq'
primary key,
config longtext collate utf8mb4_bin not null comment '설정 정보'
check (json_valid(`config`))
)
comment '설정' collate = utf8mb4_general_ci;

create table cheonil.Store
(
seq smallint unsigned auto_increment comment '매장 Seq'
primary key,
ctgSeq smallint unsigned not null comment '매장 카테고리 Seq',
placeCtgSeq smallint unsigned null comment '장소 카테고리 Seq',
name varchar(45) not null comment '매장 명',
cmt varchar(1000) null comment '기타 정보',
latitude float null comment '위도',
longitude float null comment '경도',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간',
constraint name
unique (name)
)
comment '매장' collate = utf8mb4_general_ci;

create index FK_PlaceCategory_TO_Store
on cheonil.Store (placeCtgSeq);

create index FK_StoreCategory_TO_Store
on cheonil.Store (ctgSeq);

create table cheonil.StoreCategory
(
seq smallint unsigned auto_increment comment '매장 카테고리 Seq'
primary key,
placeCtgSeq smallint unsigned null comment '장소 카테고리 Seq',
name varchar(45) not null comment '매장 카테고리 명',
options longtext collate utf8mb4_bin null comment '추가정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간',
constraint name
unique (name)
)
comment '매장 카테고리' collate = utf8mb4_general_ci;

create index FK_PlaceCategory_TO_StoreCategory
on cheonil.StoreCategory (placeCtgSeq);

create table cheonil.Supply
(
seq smallint unsigned auto_increment comment '식자재 Seq'
primary key,
name varchar(100) not null comment '식자재 명',
options longtext collate utf8mb4_bin null comment '추가 정보'
check (json_valid(`options`)),
createdAt datetime default current_timestamp() null comment '생성시간',
updatedAt datetime default current_timestamp() null comment '수정시간'
)
comment '식자재' collate = utf8mb4_general_ci;

create table cheonil.Unit
(
seq smallint unsigned auto_increment comment '단위 SEQ'
primary key,
name varchar(40) not null comment '단위',
isUnitCnt tinyint(1) default 0 not null comment '단위수량 여부'
)
comment '단위' collate = utf8mb4_general_ci;
