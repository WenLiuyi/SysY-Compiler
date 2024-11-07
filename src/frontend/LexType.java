package frontend;

public enum LexType {
    IDENFR,     //Ident
    INT_FUN_IDENFR,     //返回值为int型的函数
    CHAR_FUN_IDENFR,     //返回值为char型的函数
    VOID_FUN_IDENFR,     //无返回值（void）函数

    INT_VAR_IDENFR,     //int型变量
    INT_VAR_ARRAY_IDENFR,   //int变量数组
    CHAR_VAR_IDENFR,    //char型变量
    CHAR_VAR_ARRAY_IDENFR,  //char型变量数组

    INT_CONST_IDENFR,   //int型常量
    INT_CONST_ARRAY_IDENFR, //int型常量数组
    CHAR_CONST_IDENFR,  //char型常量
    CHAR_CONST_ARRAY_IDENFR,    //char型常量数组

    INTCON,     //IntConst,
    STRCON,     //StringConst
    CHRCON,     //CharConst

    MAINTK,     //main
    CONSTTK,    //const
    INTTK,      //int
    CHARTK,     //char
    VOIDTK,     //void
    BREAKTK,    //break
    CONTINUETK, //continue
    IFTK,       //if
    ELSETK,     //else
    NOT,        //!
    AND,        //&&
    OR,         //||
    FORTK,      //for

    GETINTTK,   //getint
    GETCHARTK,  //getchar()
    PRINTFTK,   //printf
    RETURNTK,   //return

    PLUS,       //+
    MINU,      //-
    MULT,       //*
    DIV,        // /
    MOD,        //%
    LSS,        //<
    LEQ,        //<=
    GRE,        //>
    GEQ,        //>=
    EQL,        //==
    NEQ,        //!=
    ASSIGN,     //=

    SEMICN,     //;
    COMMA,      //,
    LPARENT,    //(
    RPARENT,    //)
    LBRACK,     //[
    RBRACK,     //]
    LBRACE,     //{
    RBRACE,     //}

    NONE,      //none
}
