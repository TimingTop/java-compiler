## 解析

NFA :  就是一个节点

NFAManager :  分配 NFA 节点

自底向上 构造 NFA 最后到 连接操作，差一个 or 运算

0. term -> character | . | [...] | [^...]
1. factor -> term* | term+ | term?
2. cat_expr -> factor cat_expr | factor
3. expr -> expr OR cat_expr | cat_expr
