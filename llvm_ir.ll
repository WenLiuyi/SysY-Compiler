declare i32 @getint()
declare i32 @getchar()
declare void @putint(i32)
declare void @putch(i32)
declare void @putstr(i8*)
@.str=private unnamed_addr constant [10 x i8] c"21371295\0A\00"
define dso_local i32 @main() {
%1=alloca i32
%2=alloca i32
%3=alloca i32
call void @putstr(i8* getelementptr inbounds ([10 x i8], [10 x i8]* @.str, i64 0, i64 0))
store i32 0, i32* %1
%4=add nsw i32 0, 1
%5=load i32, i32* %1
%6=icmp sgt i32 %5, 0
%7=zext i1 %6 to i32
%8=icmp ne i32 %7, 0
br i1 %8, label %9, label %10
9:
store i32 1, i32* %1
br label %11
10:
store i32 0, i32* %1
br label %11
11:
store i32 1, i32* %2
store i32 2, i32* %3
%12=load i32, i32* %2
%13=icmp sge i32 %12, 0
%14=zext i1 %13 to i32
%15=icmp ne i32 %14, 0
br i1 %15, label %16, label %17
16:
store i32 1, i32* %2
br label %17
17:
%18=load i32, i32* %2
%19=icmp sge i32 %18, 0
%20=zext i1 %19 to i32
%21=icmp ne i32 %20, 0
br i1 %21, label %22, label %23
22:
br label %23
23:
%24=load i32, i32* %2
%25=icmp ne i32 %24, 0
%26=xor i1 %25, true
%27=zext i1 %26 to i32
%28=icmp ne i32 %27, 0
br i1 %28, label %29, label %30
29:
br label %31
30:
br label %31
31:
%32=load i32, i32* %3
%33=icmp sle i32 %32, 0
%34=zext i1 %33 to i32
%35=icmp ne i32 %34, 0
br i1 %35, label %36, label %37
36:
store i32 2, i32* %3
br label %37
37:
%38=load i32, i32* %2
%39=load i32, i32* %3
%40=icmp eq i32 %38, %39
br i1 %40, label %41, label %42
41:
store i32 1, i32* %1
br label %42
42:
%43=load i32, i32* %2
%44=load i32, i32* %3
%45=icmp ne i32 %43, %44
br i1 %45, label %46, label %47
46:
store i32 0, i32* %1
br label %47
47:
%48=load i32, i32* %2
%49=load i32, i32* %3
%50=icmp ne i32 %48, %49
br i1 %50, label %51, label %57
51:
%52=load i32, i32* %2
%53=icmp sgt i32 %52, 0
%54=zext i1 %53 to i32
%55=icmp ne i32 %54, 0
br i1 %55, label %56, label %57
56:
store i32 1, i32* %1
br label %57
57:
%58=load i32, i32* %2
%59=load i32, i32* %3
%60=icmp ne i32 %58, %59
br i1 %60, label %66, label %61
61:
%62=load i32, i32* %2
%63=icmp sgt i32 %62, 0
%64=zext i1 %63 to i32
%65=icmp ne i32 %64, 0
br i1 %65, label %66, label %67
66:
store i32 2, i32* %1
br label %67
67:
%68=load i32, i32* %2
%69=add nsw i32 %68, 1
%70=mul nsw i32 3, 4
%71=sdiv i32 %70, 3
%72=srem i32 %71, 2
%73=sub nsw i32 %69, %72
store i32 %73, i32* %2
%74=add nsw i32 1, 1
store i32 %74, i32* %2
%75=sub nsw i32 0, 1
store i32 %75, i32* %2
ret i32 0
}
