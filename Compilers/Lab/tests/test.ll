; ModuleID = 'module'
source_filename = "module"

@sort_arr = global [5 x i32] zeroinitializer

define i32 @combine(i32* %0, i32 %1, i32* %2, i32 %3) {
combine_entry:
  %arr1 = alloca i32*, align 8
  store i32* %0, i32** %arr1, align 8
  %arr1_length = alloca i32, align 4
  store i32 %1, i32* %arr1_length, align 4
  %arr2 = alloca i32*, align 8
  store i32* %2, i32** %arr2, align 8
  %arr2_length = alloca i32, align 4
  store i32 %3, i32* %arr2_length, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  %j = alloca i32, align 4
  store i32 0, i32* %j, align 4
  %k = alloca i32, align 4
  store i32 0, i32* %k, align 4
  br label %whileCondition

whileCondition:                                   ; preds = %nextBlock14, %combine_entry
  %result = alloca i32, align 4
  br label %andLeftBlock

whileBlock:                                       ; preds = %afterBlock
  %i7 = load i32, i32* %i, align 4
  %arr18 = load i32*, i32** %arr1, align 8
  %"&arr1" = getelementptr i32, i32* %arr18, i32 %i7
  %"arr1[i]" = load i32, i32* %"&arr1", align 4
  %j9 = load i32, i32* %j, align 4
  %arr210 = load i32*, i32** %arr2, align 8
  %"&arr2" = getelementptr i32, i32* %arr210, i32 %j9
  %"arr2[j]" = load i32, i32* %"&arr2", align 4
  %lessThan_11 = icmp slt i32 %"arr1[i]", %"arr2[j]"
  %zext_12 = zext i1 %lessThan_11 to i32
  %icmp_13 = icmp ne i32 0, %zext_12
  br i1 %icmp_13, label %trueBlock, label %falseBlock

nextBlock:                                        ; preds = %nextBlock14, %afterBlock
  %i31 = load i32, i32* %i, align 4
  %arr1_length32 = load i32, i32* %arr1_length, align 4
  %equal_ = icmp eq i32 %i31, %arr1_length32
  %zext_33 = zext i1 %equal_ to i32
  %icmp_34 = icmp ne i32 0, %zext_33
  br i1 %icmp_34, label %trueBlock35, label %falseBlock36

andLeftBlock:                                     ; preds = %whileCondition
  %i1 = load i32, i32* %i, align 4
  %arr1_length2 = load i32, i32* %arr1_length, align 4
  %lessThan_ = icmp slt i32 %i1, %arr1_length2
  %zext_ = zext i1 %lessThan_ to i32
  %leftResult = icmp ne i32 %zext_, 0
  store i32 %zext_, i32* %result, align 4
  br i1 %leftResult, label %andRightBlock, label %afterBlock

andRightBlock:                                    ; preds = %andLeftBlock
  %j3 = load i32, i32* %j, align 4
  %arr2_length4 = load i32, i32* %arr2_length, align 4
  %lessThan_5 = icmp slt i32 %j3, %arr2_length4
  %zext_6 = zext i1 %lessThan_5 to i32
  %rightResult = icmp ne i32 %zext_6, 0
  store i32 %zext_6, i32* %result, align 4
  br label %afterBlock

afterBlock:                                       ; preds = %andRightBlock, %andLeftBlock
  %loadFromResult = load i32, i32* %result, align 4
  %icmp_ = icmp ne i32 0, %loadFromResult
  br i1 %icmp_, label %whileBlock, label %nextBlock

trueBlock:                                        ; preds = %whileBlock
  %i15 = load i32, i32* %i, align 4
  %arr116 = load i32*, i32** %arr1, align 8
  %"&arr117" = getelementptr i32, i32* %arr116, i32 %i15
  %"arr1[i]18" = load i32, i32* %"&arr117", align 4
  %k19 = load i32, i32* %k, align 4
  %"&sort_arr" = getelementptr [5 x i32], [5 x i32]* @sort_arr, i32 0, i32 %k19
  store i32 %"arr1[i]18", i32* %"&sort_arr", align 4
  %i20 = load i32, i32* %i, align 4
  %add_ = add i32 %i20, 1
  store i32 %add_, i32* %i, align 4
  br label %nextBlock14

falseBlock:                                       ; preds = %whileBlock
  %j21 = load i32, i32* %j, align 4
  %arr222 = load i32*, i32** %arr2, align 8
  %"&arr223" = getelementptr i32, i32* %arr222, i32 %j21
  %"arr2[j]24" = load i32, i32* %"&arr223", align 4
  %k25 = load i32, i32* %k, align 4
  %"&sort_arr26" = getelementptr [5 x i32], [5 x i32]* @sort_arr, i32 0, i32 %k25
  store i32 %"arr2[j]24", i32* %"&sort_arr26", align 4
  %j27 = load i32, i32* %j, align 4
  %add_28 = add i32 %j27, 1
  store i32 %add_28, i32* %j, align 4
  br label %nextBlock14

nextBlock14:                                      ; preds = %falseBlock, %trueBlock
  %k29 = load i32, i32* %k, align 4
  %add_30 = add i32 %k29, 1
  store i32 %add_30, i32* %k, align 4
  br label %whileCondition
  br label %nextBlock

trueBlock35:                                      ; preds = %nextBlock
  br label %whileCondition38

falseBlock36:                                     ; preds = %nextBlock
  br label %whileCondition56

nextBlock37:                                      ; preds = %nextBlock58, %nextBlock40
  %arr1_length73 = load i32, i32* %arr1_length, align 4
  %arr2_length74 = load i32, i32* %arr2_length, align 4
  %add_75 = add i32 %arr1_length73, %arr2_length74
  %sub_ = sub i32 %add_75, 1
  %"&sort_arr76" = getelementptr [5 x i32], [5 x i32]* @sort_arr, i32 0, i32 %sub_
  %"sort_arr[arr1_length+arr2_length-1]" = load i32, i32* %"&sort_arr76", align 4
  ret i32 %"sort_arr[arr1_length+arr2_length-1]"

whileCondition38:                                 ; preds = %whileBlock39, %trueBlock35
  %j41 = load i32, i32* %j, align 4
  %arr2_length42 = load i32, i32* %arr2_length, align 4
  %lessThan_43 = icmp slt i32 %j41, %arr2_length42
  %zext_44 = zext i1 %lessThan_43 to i32
  %icmp_45 = icmp ne i32 0, %zext_44
  br i1 %icmp_45, label %whileBlock39, label %nextBlock40

whileBlock39:                                     ; preds = %whileCondition38
  %j46 = load i32, i32* %j, align 4
  %arr247 = load i32*, i32** %arr2, align 8
  %"&arr248" = getelementptr i32, i32* %arr247, i32 %j46
  %"arr2[j]49" = load i32, i32* %"&arr248", align 4
  %k50 = load i32, i32* %k, align 4
  %"&sort_arr51" = getelementptr [5 x i32], [5 x i32]* @sort_arr, i32 0, i32 %k50
  store i32 %"arr2[j]49", i32* %"&sort_arr51", align 4
  %k52 = load i32, i32* %k, align 4
  %add_53 = add i32 %k52, 1
  store i32 %add_53, i32* %k, align 4
  %j54 = load i32, i32* %j, align 4
  %add_55 = add i32 %j54, 1
  store i32 %add_55, i32* %j, align 4
  br label %whileCondition38
  br label %nextBlock40

nextBlock40:                                      ; preds = %whileBlock39, %whileCondition38
  br label %nextBlock37

whileCondition56:                                 ; preds = %whileBlock57, %falseBlock36
  %i59 = load i32, i32* %i, align 4
  %arr1_length60 = load i32, i32* %arr1_length, align 4
  %lessThan_61 = icmp slt i32 %i59, %arr1_length60
  %zext_62 = zext i1 %lessThan_61 to i32
  %icmp_63 = icmp ne i32 0, %zext_62
  br i1 %icmp_63, label %whileBlock57, label %nextBlock58

whileBlock57:                                     ; preds = %whileCondition56
  %i64 = load i32, i32* %i, align 4
  %arr265 = load i32*, i32** %arr2, align 8
  %"&arr266" = getelementptr i32, i32* %arr265, i32 %i64
  %"arr2[i]" = load i32, i32* %"&arr266", align 4
  %k67 = load i32, i32* %k, align 4
  %"&sort_arr68" = getelementptr [5 x i32], [5 x i32]* @sort_arr, i32 0, i32 %k67
  store i32 %"arr2[i]", i32* %"&sort_arr68", align 4
  %k69 = load i32, i32* %k, align 4
  %add_70 = add i32 %k69, 1
  store i32 %add_70, i32* %k, align 4
  %i71 = load i32, i32* %i, align 4
  %add_72 = add i32 %i71, 1
  store i32 %add_72, i32* %i, align 4
  br label %whileCondition56
  br label %nextBlock58

nextBlock58:                                      ; preds = %whileBlock57, %whileCondition56
  br label %nextBlock37
}

define i32 @main() {
main_entry:
  %a = alloca [2 x i32], align 4
  %"&0" = getelementptr [2 x i32], [2 x i32]* %a, i32 0, i32 0
  store i32 1, i32* %"&0", align 4
  %"&1" = getelementptr [2 x i32], [2 x i32]* %a, i32 0, i32 1
  store i32 5, i32* %"&1", align 4
  %b = alloca [3 x i32], align 4
  %"&01" = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 0
  store i32 1, i32* %"&01", align 4
  %"&12" = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 1
  store i32 4, i32* %"&12", align 4
  %"&2" = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 2
  store i32 14, i32* %"&2", align 4
  %"&a" = getelementptr [2 x i32], [2 x i32]* %a, i32 0, i32 0
  %"&b" = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 0
  %combine = call i32 @combine(i32* %"&a", i32 2, i32* %"&b", i32 3)
  ret i32 %combine
}
