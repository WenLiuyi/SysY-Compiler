declare i32 @getint()
declare i32 @getchar()
declare void @putint(i32)
declare void @putch(i32)
declare void @putstr(i8*)
@a=dso_local global i32 5
@arr=dso_local global [3 x i32] [i32 1, i32 2, i32 3]
define dso_local i32 @f(i32 %0) {
%2=alloca i32
store i32 %0, i32* %2
%3=load i32, i32* %2
ret i32 %3
}
define dso_local i32 @main() {
%1=alloca i32
%2=sub nsw i32 0, 1
%3=add nsw i32 %2, 3
store i32 %3, i32* %1
%4=alloca i32
%5=sub nsw i32 0, 10
%6=sub nsw i32 0, 1
%7=add nsw i32 %6, 2
%8=mul nsw i32 3, %7
%9=sdiv i32 %8, 3
%10=add nsw i32 %9, 4
%11=add nsw i32 %10, 5
%12=add nsw i32 %5, %11
%13=sub nsw i32 %12, 4
%14=getelementptr inbounds [3 x i32], [3 x i32]* @arr, i32 0, i32 1
%15=load i32, i32* %14
%16=sub nsw i32 %13, %15
store i32 %16, i32* %4
%17=load i32, i32* %1
%18=load i32, i32* %4
%19=add nsw i32 %17, %18
call void @putint(i32 %19)
%20=alloca i32
%21=mul nsw i32 1, 3
%22=sub nsw i32 0, %21
%23=add nsw i32 %22, 2
store i32 %23, i32* %20
%24=alloca i32
%25=mul nsw i32 1, 3
%26=sub nsw i32 0, %25
store i32 %26, i32* %24
ret i32 0
}
