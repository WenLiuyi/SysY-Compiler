; ModuleID = 'main.c'
source_filename = "main.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@constIntArray = dso_local constant [3 x i32] [i32 10, i32 20, i32 30], align 4
@constCharArray = dso_local constant [5 x i8] c"ABCDE", align 1
@constCharArray2 = dso_local constant [5 x i8] c"abc\00\00", align 1
@.str = private unnamed_addr constant [67 x i8] c"Function with parameters: a = %d, b = %c arr[0] = %d, str[0] = %c\0A\00", align 1
@.str.1 = private unnamed_addr constant [28 x i8] c"Sum in func_with_param: %d\0A\00", align 1
@.str.2 = private unnamed_addr constant [10 x i8] c"22373141\0A\00", align 1
@intArray = dso_local global [5 x i32] zeroinitializer, align 16
@.str.3 = private unnamed_addr constant [26 x i8] c"Negative intArray[0]: %d\0A\00", align 1
@.str.4 = private unnamed_addr constant [26 x i8] c"Positive intArray[0]: %d\0A\00", align 1
@.str.5 = private unnamed_addr constant [29 x i8] c"Quotient: %d, Remainder: %d\0A\00", align 1
@charArray = dso_local global [5 x i8] zeroinitializer, align 1
@.str.6 = private unnamed_addr constant [28 x i8] c"Sum of ASCII codes1: %d %c\0A\00", align 1
@.str.7 = private unnamed_addr constant [28 x i8] c"Sum of ASCII codes2: %d %c\0A\00", align 1
@.str.8 = private unnamed_addr constant [9 x i8] c"x1 = %d\0A\00", align 1
@.str.9 = private unnamed_addr constant [22 x i8] c"a1 = %d, as char: %c\0A\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @func_with_param(i32 noundef %0, i8 noundef signext %1, i32* noundef %2, i8* noundef %3) #0 {
  %5 = alloca i32, align 4
  %6 = alloca i8, align 1
  %7 = alloca i32*, align 8
  %8 = alloca i8*, align 8
  %9 = alloca i32, align 4
  store i32 %0, i32* %5, align 4
  store i8 %1, i8* %6, align 1
  store i32* %2, i32** %7, align 8
  store i8* %3, i8** %8, align 8
  %10 = load i32, i32* %5, align 4
  %11 = load i8, i8* %6, align 1
  %12 = sext i8 %11 to i32
  %13 = load i32*, i32** %7, align 8
  %14 = getelementptr inbounds i32, i32* %13, i64 0
  %15 = load i32, i32* %14, align 4
  %16 = load i8*, i8** %8, align 8
  %17 = getelementptr inbounds i8, i8* %16, i64 0
  %18 = load i8, i8* %17, align 1
  %19 = sext i8 %18 to i32
  %20 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([67 x i8], [67 x i8]* @.str, i64 0, i64 0), i32 noundef %10, i32 noundef %12, i32 noundef %15, i32 noundef %19)
  %21 = load i32, i32* %5, align 4
  %22 = load i8, i8* %6, align 1
  %23 = sext i8 %22 to i32
  %24 = add nsw i32 %21, %23
  %25 = load i32*, i32** %7, align 8
  %26 = getelementptr inbounds i32, i32* %25, i64 0
  %27 = load i32, i32* %26, align 4
  %28 = add nsw i32 %24, %27
  %29 = load i8*, i8** %8, align 8
  %30 = getelementptr inbounds i8, i8* %29, i64 0
  %31 = load i8, i8* %30, align 1
  %32 = sext i8 %31 to i32
  %33 = add nsw i32 %28, %32
  store i32 %33, i32* %9, align 4
  %34 = load i32, i32* %9, align 4
  %35 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([28 x i8], [28 x i8]* @.str.1, i64 0, i64 0), i32 noundef %34)
  %36 = load i32, i32* %9, align 4
  ret i32 %36
}

declare i32 @printf(i8* noundef, ...) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = alloca i8, align 1
  %5 = alloca i32, align 4
  %6 = alloca i8, align 1
  store i32 0, i32* %1, align 4
  %7 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([10 x i8], [10 x i8]* @.str.2, i64 0, i64 0))
  %8 = load i32, i32* getelementptr inbounds ([3 x i32], [3 x i32]* @constIntArray, i64 0, i64 0), align 4
  store i32 %8, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %9 = load i32, i32* getelementptr inbounds ([3 x i32], [3 x i32]* @constIntArray, i64 0, i64 1), align 4
  store i32 %9, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 1), align 4
  %10 = load i32, i32* getelementptr inbounds ([3 x i32], [3 x i32]* @constIntArray, i64 0, i64 2), align 4
  store i32 %10, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %11 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %12 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 1), align 4
  %13 = add nsw i32 %11, %12
  store i32 %13, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 3), align 4
  %14 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 3), align 4
  %15 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %16 = add nsw i32 %14, %15
  store i32 %16, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 4), align 16
  %17 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %18 = sub nsw i32 0, %17
  store i32 %18, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %19 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %20 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([26 x i8], [26 x i8]* @.str.3, i64 0, i64 0), i32 noundef %19)
  %21 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  store i32 %21, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %22 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %23 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([26 x i8], [26 x i8]* @.str.4, i64 0, i64 0), i32 noundef %22)
  %24 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 3), align 4
  %25 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %26 = sdiv i32 %24, %25
  store i32 %26, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 1), align 4
  %27 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 3), align 4
  %28 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %29 = srem i32 %27, %28
  store i32 %29, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %30 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 1), align 4
  %31 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 2), align 8
  %32 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([29 x i8], [29 x i8]* @.str.5, i64 0, i64 0), i32 noundef %30, i32 noundef %31)
  %33 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray, i64 0, i64 0), align 1
  %34 = sext i8 %33 to i32
  %35 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray, i64 0, i64 1), align 1
  %36 = sext i8 %35 to i32
  %37 = add nsw i32 %34, %36
  %38 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray, i64 0, i64 2), align 1
  %39 = sext i8 %38 to i32
  %40 = add nsw i32 %37, %39
  %41 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray, i64 0, i64 3), align 1
  %42 = sext i8 %41 to i32
  %43 = add nsw i32 %40, %42
  %44 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray, i64 0, i64 4), align 1
  %45 = sext i8 %44 to i32
  %46 = add nsw i32 %43, %45
  %47 = trunc i32 %46 to i8
  store i8 %47, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @charArray, i64 0, i64 0), align 1
  %48 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @charArray, i64 0, i64 0), align 1
  %49 = sext i8 %48 to i32
  %50 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @charArray, i64 0, i64 0), align 1
  %51 = sext i8 %50 to i32
  %52 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([28 x i8], [28 x i8]* @.str.6, i64 0, i64 0), i32 noundef %49, i32 noundef %51)
  %53 = load i32, i32* getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), align 16
  %54 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @charArray, i64 0, i64 0), align 1
  %55 = call i32 @func_with_param(i32 noundef %53, i8 noundef signext %54, i32* noundef getelementptr inbounds ([5 x i32], [5 x i32]* @intArray, i64 0, i64 0), i8* noundef getelementptr inbounds ([5 x i8], [5 x i8]* @charArray, i64 0, i64 0))
  store i32 %55, i32* %2, align 4
  %56 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 0), align 1
  %57 = sext i8 %56 to i32
  %58 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 1), align 1
  %59 = sext i8 %58 to i32
  %60 = add nsw i32 %57, %59
  %61 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 2), align 1
  %62 = sext i8 %61 to i32
  %63 = add nsw i32 %60, %62
  %64 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 3), align 1
  %65 = sext i8 %64 to i32
  %66 = add nsw i32 %63, %65
  %67 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 4), align 1
  %68 = sext i8 %67 to i32
  %69 = add nsw i32 %66, %68
  store i32 %69, i32* %3, align 4
  %70 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 0), align 1
  %71 = sext i8 %70 to i32
  %72 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 1), align 1
  %73 = sext i8 %72 to i32
  %74 = add nsw i32 %71, %73
  %75 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 2), align 1
  %76 = sext i8 %75 to i32
  %77 = add nsw i32 %74, %76
  %78 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 3), align 1
  %79 = sext i8 %78 to i32
  %80 = add nsw i32 %77, %79
  %81 = load i8, i8* getelementptr inbounds ([5 x i8], [5 x i8]* @constCharArray2, i64 0, i64 4), align 1
  %82 = sext i8 %81 to i32
  %83 = add nsw i32 %80, %82
  %84 = trunc i32 %83 to i8
  store i8 %84, i8* %4, align 1
  %85 = load i32, i32* %3, align 4
  %86 = load i8, i8* %4, align 1
  %87 = sext i8 %86 to i32
  %88 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([28 x i8], [28 x i8]* @.str.7, i64 0, i64 0), i32 noundef %85, i32 noundef %87)
  store i32 107, i32* %5, align 4
  %89 = load i32, i32* %5, align 4
  %90 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([9 x i8], [9 x i8]* @.str.8, i64 0, i64 0), i32 noundef %89)
  store i8 41, i8* %6, align 1
  %91 = load i8, i8* %6, align 1
  %92 = sext i8 %91 to i32
  %93 = load i8, i8* %6, align 1
  %94 = sext i8 %93 to i32
  %95 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([22 x i8], [22 x i8]* @.str.9, i64 0, i64 0), i32 noundef %92, i32 noundef %94)
  ret i32 0
}

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #1 = { "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2, !3, !4}
!llvm.ident = !{!5}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{i32 7, !"uwtable", i32 1}
!4 = !{i32 7, !"frame-pointer", i32 2}
!5 = !{!"Ubuntu clang version 14.0.0-1ubuntu1.1"}
