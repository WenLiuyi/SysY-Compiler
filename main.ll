; ModuleID = 'main.c'
source_filename = "main.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@.str = private unnamed_addr constant [13 x i8] c"%d\0A%d\0A%d\0A%d\0A\00", align 1
@.str.1 = private unnamed_addr constant [10 x i8] c"19182620\0A\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @de() #0 {
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @keke(i32 noundef %0, i32 noundef %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  store i32 %1, i32* %4, align 4
  %5 = load i32, i32* %3, align 4
  %6 = load i32, i32* %4, align 4
  %7 = add nsw i32 %5, %6
  store i32 %7, i32* %3, align 4
  ret i32 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @jian() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = call i32 (...) @getint()
  store i32 %4, i32* %1, align 4
  %5 = call i32 (...) @getint()
  store i32 %5, i32* %2, align 4
  %6 = load i32, i32* %1, align 4
  %7 = load i32, i32* %2, align 4
  %8 = sub nsw i32 %6, %7
  store i32 %8, i32* %3, align 4
  %9 = load i32, i32* %3, align 4
  ret i32 %9
}

declare i32 @getint(...) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  %9 = alloca i32, align 4
  %10 = alloca i32, align 4
  %11 = alloca i32, align 4
  %12 = alloca i32, align 4
  %13 = alloca i32, align 4
  %14 = alloca i32, align 4
  %15 = alloca i32, align 4
  %16 = alloca i32, align 4
  %17 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  store i32 1, i32* %8, align 4
  store i32 -1, i32* %13, align 4
  store i32 2, i32* %14, align 4
  store i32 0, i32* %17, align 4
  %18 = load i32, i32* %7, align 4
  %19 = icmp ne i32 %18, 0
  br i1 %19, label %20, label %23

20:                                               ; preds = %0
  %21 = load i32, i32* %8, align 4
  %22 = add nsw i32 %21, 1
  store i32 %22, i32* %8, align 4
  br label %23

23:                                               ; preds = %20, %0
  %24 = load i32, i32* %14, align 4
  %25 = load i32, i32* %10, align 4
  %26 = add nsw i32 %25, 1
  %27 = add nsw i32 %24, %26
  store i32 %27, i32* %13, align 4
  br label %28

28:                                               ; preds = %29, %23
  br i1 false, label %29, label %30

29:                                               ; preds = %28
  br label %28

30:                                               ; preds = %28
  br label %31

31:                                               ; preds = %30
  br i1 true, label %32, label %33

32:                                               ; preds = %31
  br label %33

33:                                               ; preds = %32, %31
  %34 = load i32, i32* %4, align 4
  %35 = load i32, i32* %5, align 4
  %36 = icmp eq i32 %34, %35
  br i1 %36, label %37, label %57

37:                                               ; preds = %33
  %38 = load i32, i32* %5, align 4
  %39 = load i32, i32* %6, align 4
  %40 = icmp sge i32 %38, %39
  br i1 %40, label %41, label %56

41:                                               ; preds = %37
  %42 = load i32, i32* %6, align 4
  %43 = load i32, i32* %7, align 4
  %44 = icmp sle i32 %42, %43
  br i1 %44, label %45, label %55

45:                                               ; preds = %41
  %46 = load i32, i32* %7, align 4
  %47 = load i32, i32* %8, align 4
  %48 = icmp ne i32 %46, %47
  br i1 %48, label %49, label %54

49:                                               ; preds = %45
  %50 = load i32, i32* %4, align 4
  %51 = icmp sgt i32 %50, 1
  br i1 %51, label %52, label %53

52:                                               ; preds = %49
  store i32 1, i32* %2, align 4
  br label %53

53:                                               ; preds = %52, %49
  br label %54

54:                                               ; preds = %53, %45
  br label %55

55:                                               ; preds = %54, %41
  br label %56

56:                                               ; preds = %55, %37
  br label %57

57:                                               ; preds = %56, %33
  %58 = load i32, i32* %2, align 4
  %59 = load i32, i32* %3, align 4
  %60 = call i32 @keke(i32 noundef %58, i32 noundef %59)
  %61 = load i32, i32* %5, align 4
  %62 = load i32, i32* %6, align 4
  %63 = load i32, i32* %7, align 4
  %64 = load i32, i32* %8, align 4
  %65 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([13 x i8], [13 x i8]* @.str, i64 0, i64 0), i32 noundef %61, i32 noundef %62, i32 noundef %63, i32 noundef %64)
  %66 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([10 x i8], [10 x i8]* @.str.1, i64 0, i64 0))
  %67 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([10 x i8], [10 x i8]* @.str.1, i64 0, i64 0))
  %68 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([10 x i8], [10 x i8]* @.str.1, i64 0, i64 0))
  ret i32 0
}

declare i32 @printf(i8* noundef, ...) #1

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
