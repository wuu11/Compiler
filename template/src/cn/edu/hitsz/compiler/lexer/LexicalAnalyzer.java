package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.Iterator;
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
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private String content;

    private final List<Token> tokenList = new ArrayList<>();

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        content = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // 自动机实现的词法分析过程
        char ch;
        int i;
        String str;
        for (i = 0; i < content.length(); i++) {
            ch = content.charAt(i);
            if (ch == ' ' || ch == '\n' || ch == '\t') {
                continue;
            }
            if (Character.isLetter(ch)) {
                str = String.valueOf(ch);
                for (i = i+1; i < content.length(); i++) {
                    ch = content.charAt(i);
                    if (!Character.isLetterOrDigit(ch) && ch != '_') {
                        i--;
                        break;
                    } else {
                        str += ch;
                    }
                }
                if ("int".equals(str) || "return".equals(str)) {
                    tokenList.add(Token.simple(str));
                } else {
                    tokenList.add(Token.normal("id", str));
                    if (!symbolTable.has(str)) {
                        symbolTable.add(str);
                    }
                }
            } else if (Character.isDigit(ch)) {
                str = String.valueOf(ch);
                for (i = i+1; i < content.length(); i++) {
                    ch = content.charAt(i);
                    if (!Character.isDigit(ch)) {
                        i--;
                        break;
                    } else {
                        str += ch;
                    }
                }
                tokenList.add(Token.normal("IntConst", str));
            } else if (ch == ';') {
                tokenList.add(Token.simple("Semicolon"));
            } else {
                str = String.valueOf(ch);
                tokenList.add(Token.simple(str));
            }
        }
        tokenList.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return new Iterable<Token>() {
            @Override
            public Iterator<Token> iterator() {
                return tokenList.iterator();
            }
        };
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
