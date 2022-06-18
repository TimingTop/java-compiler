## 解析

NFA :  就是一个节点

NFAManager :  分配 NFA 节点

自底向上 构造 NFA 最后到 连接操作，差一个 or 运算

term -> character | . | [...] | [^...]
factor -> term* | term+ | term?
cat_expr -> factor factor
== cat_expr -> factor cat_expr 