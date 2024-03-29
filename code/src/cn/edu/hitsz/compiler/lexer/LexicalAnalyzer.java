package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {

    // 符号表
    private final SymbolTable symbolTable;
    // token序列
    private final List<Token> tokenList = new ArrayList<>();
    // 输入源代码，初始为空，读取文件时追加
    private String inputCode = "";
    /**
     * 状态机状态,对应DFA状态转换图
     * Status: INITIAL      状态0
     * Status: IDENTIFIER   读入第一个字符为letter，对应状态14，预计为标识符
     * Status: INTEGER      读入第一个字符为digit ，对应状态16，预计为整常数
     */
    public enum IterateStatus{INITIAL, IDENTIFIER, INTEGER};
    // 构造函数：初始化符号表
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputCode += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        IterateStatus currentStatus = IterateStatus.INITIAL;
        StringBuilder bufferedString = new StringBuilder();
        char ch;
        for(int i = 0; i< inputCode.length(); i++){
            ch = inputCode.charAt(i);       // Character at the position i of the string
            System.out.println("~DEBUG~ Current Status: " + currentStatus + ", Character = " + ch);
            switch (currentStatus){
                case INITIAL : {
                    if(isDelimiter(ch)){    // Ignore delimiter
                        currentStatus = IterateStatus.INITIAL;
                    }
                    else if(isLetter(ch)){  // A token start with a letter
                        bufferedString.append(ch);
                        currentStatus = IterateStatus.IDENTIFIER;
                    }
                    else if(isDigit(ch)){   // A token start with a digit
                        bufferedString.append(ch);
                        currentStatus = IterateStatus.INTEGER;
                    }
                    else {
                        switch (ch){
                            case '=': tokenList.add(Token.normal(TokenKind.fromString("="), "")); break;
                            case '(': tokenList.add(Token.normal(TokenKind.fromString("("), "")); break;
                            case ')': tokenList.add(Token.normal(TokenKind.fromString(")"), "")); break;
                            case '+': tokenList.add(Token.normal(TokenKind.fromString("+"), "")); break;
                            case '-': tokenList.add(Token.normal(TokenKind.fromString("-"), "")); break;
                            case '*': tokenList.add(Token.normal(TokenKind.fromString("*"), "")); break;
                            case '/': tokenList.add(Token.normal(TokenKind.fromString("/"), "")); break;
                            case ';': tokenList.add(Token.normal(TokenKind.fromString("Semicolon"), "")); break;
                            default: break;
                        }
                        currentStatus = IterateStatus.INITIAL;
                    }
                    break;
                }

                case IDENTIFIER : {
                    if(isLetter(ch) || isDigit(ch)){
                        bufferedString.append(ch);
                        currentStatus = IterateStatus.IDENTIFIER;
                    }
                    else {
                        if (bufferedString.toString().equals("int")) {
                            tokenList.add(Token.normal(TokenKind.fromString("int"), ""));
                        }
                        else if (bufferedString.toString().equals("return")){
                            tokenList.add(Token.normal(TokenKind.fromString("return"), ""));
                        }
                        else {      // Identifier defined by user
                            tokenList.add(Token.normal(TokenKind.fromString("id"), bufferedString.toString()));
                            symbolTable.add(bufferedString.toString());
                        }
                        i--;        // Retrace the index for once
                        bufferedString = new StringBuilder();
                        currentStatus = IterateStatus.INITIAL;
                    }
                    break;
                }

                case INTEGER : {
                    if (isDigit(ch)) {
                        currentStatus = IterateStatus.INTEGER;
                        bufferedString.append(ch);
                    }
                    else {
                        tokenList.add(Token.normal(TokenKind.fromString("IntConst"), bufferedString.toString()));
                        i--;        // Retrace the index for once
                        bufferedString = new StringBuilder();
                        currentStatus = IterateStatus.INITIAL;
                    }
                    break;
                }
                default: break;
            }
        }
        tokenList.add(Token.normal(TokenKind.eof(), ""));
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokenList() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokenList().spliterator(), false).map(Token::toString).toList()
        );
    }

    private boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private boolean isDigit(char ch) {
        return (ch >= '0' && ch <= '9');
    }

    private boolean isDelimiter(char ch){
        return (ch == '\n' || ch == ' ' || ch == '\t');
    }
}
