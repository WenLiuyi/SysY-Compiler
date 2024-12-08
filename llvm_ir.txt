declare i32 @getint()
declare i32 @getchar()
declare void @putint(i32)
declare void @putch(i32)
declare void @putstr(i8*)
@const_int_a=dso_local constant i32 0
@const_int_b1=dso_local constant i32 1
@const_int_b2=dso_local constant [5 x i32] [i32 0, i32 1, i32 2, i32 3, i32 4]
@str=dso_local constant [10 x i8] c"hello!\00\00\00\00"
@char_a=dso_local global i8 97
@char_b1=dso_local global i8 98
@char_b2=dso_local global [5 x i8] [i8 97, i8 98, i8 99, i8 100, i8 101]
@char_b3=dso_local global [10 x i8] c"world!\00\00\00\00"
@int_b2=dso_local global [5 x i32]  zeroinitializer
@.str=private unnamed_addr constant [2 x i8] c" \00"
@.str.1=private unnamed_addr constant [2 x i8] c"\0A\00"
@.str.2=private unnamed_addr constant [32 x i8] c"test_stmt_getint_getchar_printf\00"
@.str.3=private unnamed_addr constant [2 x i8] c"\0A\00"
@.str.4=private unnamed_addr constant [24 x i8] c"test_Lval_exp success!\0A\00"
@.str.5=private unnamed_addr constant [27 x i8] c"test_primary_exp success!\0A\00"
@.str.6=private unnamed_addr constant [25 x i8] c"test_unary_exp success!\0A\00"
@.str.7=private unnamed_addr constant [23 x i8] c"test_mul_exp success!\0A\00"
@.str.8=private unnamed_addr constant [23 x i8] c"test_add_exp_success!\0A\00"
@.str.9=private unnamed_addr constant [23 x i8] c"test_rel_exp success!\0A\00"
@.str.10=private unnamed_addr constant [21 x i8] c"test_Eqexp success!\0A\00"
@.str.11=private unnamed_addr constant [20 x i8] c"test_Lexp success!\0A\00"
@.str.12=private unnamed_addr constant [10 x i8] c"22371236\0A\00"
define dso_local void @print_int_arr(i32* %0, i32 %1) {
%3=alloca i32*
%4=alloca i32
store i32* %0, i32** %3
store i32 %1, i32* %4
%5=alloca i32
store i32 0, i32* %5
store i32 0, i32* %5
br label %6
6:
%7=load i32, i32* %5
%8=load i32, i32* %4
%9=icmp slt i32 %7, %8
%10=zext i1 %9 to i32
%11=icmp ne i32 %10, 0
br i1 %11, label %12, label %20
12:
%13=load i32*, i32** %3
%14=load i32, i32* %5
%15=getelementptr inbounds i32, i32* %13, i32 %14
%16=load i32, i32* %15
call void @putint(i32 %16)
call void @putstr(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @.str, i64 0, i64 0))
br label %17
17:
%18=load i32, i32* %5
%19=add nsw i32 %18, 1
store i32 %19, i32* %5
br label %6
20:
call void @putstr(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @.str.1, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_stmt_1_3() {
store i8 65, i8* @char_a
%1=add nsw i32 1, 1
ret void
ret void
}
define dso_local void @test_stmt_if_else() {
%1=icmp slt i32 1, 2
%2=zext i1 %1 to i32
%3=icmp ne i32 %2, 0
br i1 %3, label %4, label %5
4:
br label %5
5:
%6=icmp sgt i32 1, 2
%7=zext i1 %6 to i32
%8=icmp ne i32 %7, 0
br i1 %8, label %9, label %10
9:
br label %11
10:
br label %11
11:
ret void
ret void
}
define dso_local void @test_stmt_for_break_continue() {
%1=alloca i32
store i32 0, i32* %1
%2=alloca i32
store i32 1, i32* %2
store i32 0, i32* %1
br label %3
3:
%4=load i32, i32* %1
%5=load i32, i32* %2
%6=icmp slt i32 %4, %5
%7=zext i1 %6 to i32
%8=icmp ne i32 %7, 0
br i1 %8, label %9, label %15
9:
%10=load i32, i32* %1
%11=add nsw i32 %10, 1
store i32 %11, i32* %1
br label %12
12:
%13=load i32, i32* %1
%14=add nsw i32 %13, 1
store i32 %14, i32* %1
br label %3
15:
store i32 0, i32* %1
br label %16
16:
%17=load i32, i32* %1
%18=load i32, i32* %2
%19=icmp slt i32 %17, %18
%20=zext i1 %19 to i32
%21=icmp ne i32 %20, 0
br i1 %21, label %22, label %26
22:
br label %23
23:
%24=load i32, i32* %1
%25=add nsw i32 %24, 1
store i32 %25, i32* %1
br label %16
26:
store i32 0, i32* %1
br label %27
27:
br label %31
28:
%29=load i32, i32* %1
%30=add nsw i32 %29, 1
store i32 %30, i32* %1
br label %27
31:
store i32 0, i32* %1
br label %32
32:
%33=load i32, i32* %1
%34=load i32, i32* %2
%35=icmp slt i32 %33, %34
%36=zext i1 %35 to i32
%37=icmp ne i32 %36, 0
br i1 %37, label %38, label %41
38:
%39=load i32, i32* %1
%40=add nsw i32 %39, 1
store i32 %40, i32* %1
br label %32
41:
br label %42
42:
br label %46
43:
%44=load i32, i32* %1
%45=add nsw i32 %44, 1
store i32 %45, i32* %1
br label %42
46:
store i32 0, i32* %1
br label %47
47:
%48=load i32, i32* %1
%49=load i32, i32* %2
%50=icmp slt i32 %48, %49
%51=zext i1 %50 to i32
%52=icmp ne i32 %51, 0
br i1 %52, label %53, label %54
53:
br label %54
54:
store i32 0, i32* %1
br label %55
55:
br label %56
56:
br label %57
57:
br label %58
58:
ret void
ret void
}
define dso_local void @test_stmt_return_null() {
ret void
ret void
}
define dso_local i32 @test_stmt_return_exp() {
ret i32 1
}
define dso_local void @test_stmt_getint_getchar_printf() {
%1=alloca i32
store i32 1, i32* %1
%2=alloca i8
store i8 97, i8* %2
%3=call i32 @getint()
store i32 %3, i32* %1
%4=call i32 @getchar()
%5=trunc i32 %4 to i8
store i8 %5, i8* %2
call void @putstr(i8* getelementptr inbounds ([32 x i8], [32 x i8]* @.str.2, i64 0, i64 0))
%6=zext i8 115 to i32
call void @putch(i32 %6)
%7=zext i8 117 to i32
call void @putch(i32 %7)
%8=zext i8 99 to i32
call void @putch(i32 %8)
%9=zext i8 99 to i32
call void @putch(i32 %9)
%10=zext i8 101 to i32
call void @putch(i32 %10)
%11=zext i8 115 to i32
call void @putch(i32 %11)
%12=zext i8 115 to i32
call void @putch(i32 %12)
call void @putstr(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @.str.3, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_block() {
ret void
ret void
}
define dso_local i32 @add_int(i32 %0, i32 %1) {
%3=alloca i32
%4=alloca i32
store i32 %0, i32* %3
store i32 %1, i32* %4
%5=load i32, i32* %3
%6=load i32, i32* %4
%7=add nsw i32 %5, %6
ret i32 %7
}
define dso_local i8 @read_char(i8 %0) {
%2=alloca i8
store i8 %0, i8* %2
%3=load i8, i8* %2
ret i8 %3
}
define dso_local void @test_Lval_exp() {
%1=load i32, i32* @const_int_a
%2=load i8, i8* @char_a
%3=zext i8 %2 to i32
%4=getelementptr inbounds [5 x i8], [5 x i8]* @char_b2, i32 0, i32 0
%5=load i8, i8* %4
%6=zext i8 %5 to i32
call void @putstr(i8* getelementptr inbounds ([24 x i8], [24 x i8]* @.str.4, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_primary_exp() {
%1=load i8, i8* @char_a
%2=zext i8 %1 to i32
%3=zext i8 97 to i32
call void @putstr(i8* getelementptr inbounds ([27 x i8], [27 x i8]* @.str.5, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_func_int(i32 %0) {
%2=alloca i32
store i32 %0, i32* %2
ret void
ret void
}
define dso_local void @test_func_int_arr(i32* %0) {
%2=alloca i32*
store i32* %0, i32** %2
ret void
ret void
}
define dso_local void @test_func_mul_int(i32 %0, i32 %1) {
%3=alloca i32
%4=alloca i32
store i32 %0, i32* %3
store i32 %1, i32* %4
ret void
ret void
}
define dso_local void @test_unary_exp() {
call void @test_func_int(i32 0)
%1=getelementptr inbounds [5 x i32], [5 x i32]* @int_b2, i32 0, i32 0
%2=load i32, i32* %1
call void @test_func_int(i32 %2)
%3=getelementptr inbounds [5 x i32], [5 x i32]* @int_b2, i32 0, i32 0
call void @test_func_int_arr(i32* %3)
call void @test_func_mul_int(i32 0, i32 1)
%4=sub nsw i32 0, 1
%5=icmp ne i32 1, 0
%6=xor i1 %5, true
%7=zext i1 %6 to i32
%8=icmp ne i32 %7, 0
br i1 %8, label %9, label %10
9:
br label %10
10:
call void @putstr(i8* getelementptr inbounds ([25 x i8], [25 x i8]* @.str.6, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_mul_exp() {
%1=mul nsw i32 1, 1
%2=sdiv i32 1, 1
%3=srem i32 1, 1
call void @putstr(i8* getelementptr inbounds ([23 x i8], [23 x i8]* @.str.7, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_add_exp() {
%1=add nsw i32 1, 1
%2=sub nsw i32 1, 1
call void @putstr(i8* getelementptr inbounds ([23 x i8], [23 x i8]* @.str.8, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_rel_exp() {
%1=icmp slt i32 1, 2
%2=zext i1 %1 to i32
%3=icmp ne i32 %2, 0
br i1 %3, label %4, label %5
4:
br label %5
5:
%6=icmp sgt i32 1, 2
%7=zext i1 %6 to i32
%8=icmp ne i32 %7, 0
br i1 %8, label %9, label %10
9:
br label %10
10:
%11=icmp sle i32 1, 2
%12=zext i1 %11 to i32
%13=icmp ne i32 %12, 0
br i1 %13, label %14, label %15
14:
br label %15
15:
%16=icmp sge i32 1, 2
%17=zext i1 %16 to i32
%18=icmp ne i32 %17, 0
br i1 %18, label %19, label %20
19:
br label %20
20:
call void @putstr(i8* getelementptr inbounds ([23 x i8], [23 x i8]* @.str.9, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_Eqexp() {
%1=icmp eq i32 1, 1
br i1 %1, label %2, label %3
2:
br label %3
3:
%4=icmp ne i32 1, 1
br i1 %4, label %5, label %6
5:
br label %6
6:
call void @putstr(i8* getelementptr inbounds ([21 x i8], [21 x i8]* @.str.10, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test_Lexp() {
call void @putstr(i8* getelementptr inbounds ([20 x i8], [20 x i8]* @.str.11, i64 0, i64 0))
ret void
ret void
}
define dso_local void @test() {
call void @test_stmt_1_3()
call void @test_stmt_for_break_continue()
call void @test_stmt_getint_getchar_printf()
call void @test_stmt_if_else()
%1=call i32 @test_stmt_return_exp()
call void @test_stmt_return_null()
call void @test_Eqexp()
call void @test_Lval_exp()
call void @test_add_exp()
call void @test_mul_exp()
call void @test_primary_exp()
call void @test_rel_exp()
call void @test_unary_exp()
ret void
}
define dso_local i32 @main() {
call void @putstr(i8* getelementptr inbounds ([10 x i8], [10 x i8]* @.str.12, i64 0, i64 0))
call void @test()
ret i32 0
}
