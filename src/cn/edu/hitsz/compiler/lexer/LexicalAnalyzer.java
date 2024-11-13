package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String s;
    public List<Token> tokens = new ArrayList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    enum Status {
        SKIP,
        PUNCTUATION,
        LETTER,
        DIGIT,
        ERROR,
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     * @throws IOException
     */
    public void loadFile(String path) throws IOException {
        var buffer = new BufferedReader(Files.newBufferedReader(Paths.get(path)));
        this.s = buffer.lines().collect(Collectors.joining("\n"));
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     * 
     * @throws IOException
     */
    public void run() throws IOException {

        Status now;
        for (int i = 0; i < this.s.length(); i++) {
            char c = this.s.charAt(i);
            // just skip special chars
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                now = Status.SKIP;
            } else if (c == ',' || c == ';' || c == '=' || c == '+' || c == '-' || c == '*' || c == '/' || c == '('
                    || c == ')') {
                now = Status.PUNCTUATION;
            } else if (Character.isLetter(c)) {
                now = Status.LETTER;
            } else if (Character.isDigit(c)) {
                now = Status.DIGIT;
            } else {
                now = Status.ERROR;
            }

            switch (now) {
                case SKIP -> {}
                case PUNCTUATION -> {
                    if (c == ';')
                        tokens.add(Token.simple("Semicolon"));
                    else
                        tokens.add(Token.simple(String.valueOf(c)));

                }
                case LETTER -> {
                    int p = i;
                    while (p + 1 < this.s.length() && Character.isLetterOrDigit(this.s.charAt(p + 1))) {
                        ++p;
                    }
                    String v = this.s.substring(i, p + 1);
                    if (TokenKind.isAllowed(v)) {
                        tokens.add(Token.simple(v));
                    } else {
                        tokens.add(Token.normal("id", v));
                        if (!symbolTable.has(v)) {
                            symbolTable.add(v);
                        }
                    }
                    i = p;

                }
                case DIGIT -> {
                    int p = i;
                    while (p + 1 < this.s.length() && Character.isDigit(this.s.charAt(p + 1))) {
                        ++p;
                    }
                    tokens.add(Token.normal("IntConst", this.s.substring(i, p + 1)));
                    i = p;

                }
                case ERROR -> {
                    System.out.println("Error: invalid char " + c);
                }

            }

        }
        tokens.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // DONE: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList());
    }
}
