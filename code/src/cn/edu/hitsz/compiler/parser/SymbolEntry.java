package cn.edu.hitsz.compiler.parser;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;

class SymbolEntry {
    Token token;
    NonTerminal variable;

    private SymbolEntry(Token token, NonTerminal variable){
        this.token = token;
        this.variable = variable;
    }

    public SymbolEntry(Token token){
        this(token, null);
    }

    public SymbolEntry(NonTerminal variable){
        this(null, variable);
    }

    public Token getToken() {
        return token;
    }

    public boolean isToken(){
        return token != null;
    }
}

