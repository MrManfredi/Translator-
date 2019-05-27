package lexer;

import errors.lexical.*;

import java.util.*;

public class LexicalAnalyzer {
    static private Map<String, Integer> keywords;
    static private ArrayList<Character> OP;
    private List<LexicalError> lexicalErrors;
    private ArrayList<Lexeme> tokenTable;
    private ArrayList<Identifier> identTable;
    private ArrayList<Constant> constTable;
    private List<Label> labelTable;
    private StringBuilder builder;
    private String code;
    private int currentCharIndex;
    private int indexIdent;
    private int indexConst;
    private int indexLabel;
    private int currentId;
    private int line;
    private char tmp;
    private String lastType; // using only after isDeclaration() method

    public LexicalAnalyzer()
    {
        keywords = new HashMap<>();
        keywords.put("=", 1);
        keywords.put("==", 2);
        keywords.put("!=", 3);
        keywords.put(">=", 4);
        keywords.put("<=", 5);
        keywords.put(">>", 6);
        keywords.put("<<", 7);
        keywords.put("in", 8);
        keywords.put("out", 9);
        keywords.put("repeat", 10);
        keywords.put("until", 11);
        keywords.put("if", 12);
        keywords.put("int", 13);
        keywords.put("(", 14);
        keywords.put(")", 15);
        keywords.put("{", 16);
        keywords.put("}", 17);
        keywords.put("+", 18);
        keywords.put("-", 19);
        keywords.put("/", 20);
        keywords.put("*", 21);
        keywords.put(",", 22);
        keywords.put("?", 23);
        keywords.put(":", 24);
        keywords.put("goto", 25);
        keywords.put(">", 26);
        keywords.put("<", 27);
        keywords.put("not", 28);
        keywords.put("or", 29);
        keywords.put("and", 30);
        keywords.put("\n", 31);
        OP = new ArrayList<Character>(Arrays.asList('+', '*', '/', '^', ',', '?', ':', '(', ')', '{', '}'));

        lexicalErrors = new ArrayList<>();
        tokenTable = new ArrayList<>();
        identTable = new ArrayList<>();
        constTable = new ArrayList<>();
        labelTable = new ArrayList<>();
        builder = new StringBuilder();
    }

    public List<LexicalError> getLexicalErrors() {
        return lexicalErrors;
    }

    public ArrayList<Lexeme> getTokenTable() {
        return tokenTable;
    }

    public ArrayList<Identifier> getIdentTable() {
        return identTable;
    }

    public ArrayList<Constant> getConstTable() {
        return constTable;
    }

    public List<Label> getLabelTable() {
        return labelTable;
    }

    public void init()
    {
        currentCharIndex = 0;
        indexIdent = 0;
        indexConst = 0;
        indexLabel = 0;
        currentId = 1;
        line = 1;

        lexicalErrors.clear();
        tokenTable.clear();
        identTable.clear();
        constTable.clear();
        labelTable.clear();
    }

    public void run(String text)
    {
        init();
        code = text + " ";
        state1();
        checkLabelErrors();
        if (!lexicalErrors.isEmpty())
        {
            tokenTable.clear();
            identTable.clear();
            constTable.clear();
            labelTable.clear();
        }
    }

    private void state1()
    {
        tmp = code.charAt(currentCharIndex);
        while (hasNextChar()) {
            if (Character.isLetter(tmp)) {
                state2();   // identifier
            } else if (tmp == '-') {
                state3();   // -
            } else if (Character.isDigit(tmp)) {
                state4();   // constant
            } else if (OP.contains(tmp)) {
                state5();   // OP
            } else if (tmp == '=') {
                state6();   // =
            } else if (tmp == '<') {
                state7();   // <
            } else if (tmp == '>') {
                state8();   // >
            } else if (tmp == '!') {
                state9();   // !
            } else if (tmp == ' ' || tmp == '\t') {
                if (hasNextChar())
                {
                    nextChar();
                }
            }
            else if (tmp == '\n') {
                state10();
                line++;
                if (hasNextChar())
                {
                    nextChar();
                }
            }
            else {
                lexicalErrors.add(new UnknownSymbolError(tmp, line));
                nextChar();
            }
            clearBuilder();
        }
    }

    private void state2()   // identifier
    {
        nextChar();
        if (Character.isLetter(tmp) || Character.isDigit(tmp)) {
            state2();
        } else if (tmp == ':'){
            state11();  // label
        }
        else {
            if (keywords.containsKey(builder.toString()))
            {
                addToken();
            }
            else
            {
                Identifier temp;
                if (isExistIdentifier(builder.toString()))
                {
                    // перевірка на повторну декларацію
                    if (isDeclaration())
                    {
                        lexicalErrors.add(new VariableReDeclarationError(line, builder.toString()));
                    }
                    // уже задекларований ідентифікатор (індекс і тип беруться з таблиці)
                    temp = new Identifier(builder.toString(), getIdentifier(builder.toString()).getIndex(), getIdentifier(builder.toString()).getType());
                }
                else
                {
                    if (isDeclaration())
                    {
                        temp = new Identifier(builder.toString(), ++indexIdent, lastType);
                    }
                    else
                    {
                        lexicalErrors.add(new VariableUsedWithoutDeclarationError(line, builder.toString()));
                        temp = new Identifier(builder.toString(), ++indexIdent, "Not Declared");
                    }
                    // creating new identifier
                    identTable.add(temp);
                }
                addToken(temp);
            }
        }
    }

    private void state3()   // -
    {
        nextChar();
        int lastCode = tokenTable.get(tokenTable.size() - 1).getCode();
        if (lastCode == LexemeType.IDENT.getValue() || lastCode == LexemeType.CONST.getValue()) {
            addToken();
        } else if (Character.isDigit(tmp) ){
            state4();   // constant
        }
        else {
            addToken();
        }
    }

    private void state4()   // constant
    {
        nextChar();
        if (Character.isDigit(tmp))
        {
            state4();
        }
        else
        {
            Constant temp;
            if (isExistConstant(builder.toString()))
            {
                temp = new Constant(builder.toString(), getConstant(builder.toString()).getIndex());
            }
            else
            {
                temp = new Constant(builder.toString(), ++indexConst);
                constTable.add(temp);
            }
            addToken(temp);
        }
    }

    private void state5()   // OP
    {
        nextChar();
        addToken();
    }

    private void state6()   // =
    {
        nextChar();
        if(tmp == '=')
        {
            state5();  // ==
        }
        else
        {
            addToken();
        }
    }

    private void state7()   // <
    {
        nextChar();
        if(tmp == '=' || tmp == '<')
        {
            state5();  // <= or <<
        }
        else
        {
            addToken();
        }
    }

    private void state8()   // >
    {
        nextChar();
        if(tmp == '=' || tmp == '>')
        {
            state5();  // >= or >>
        }
        else
        {
            addToken();
        }
    }

    private void state9()   // !
    {
        nextChar();
        if(tmp == '=')
        {
            state5();  // !=
        }
        else
        {
            lexicalErrors.add(new ExpectedSymbolError(line, "!", "="));
        }
    }

    private void state10()
    {
        tokenTable.add(new Lexeme(currentId++, line, "¶", keywords.get("\n"), null ));
    }

    private void state11()    // label
    {
        nextChar();
        Label temp;
        if (isExistLabel(builder.toString()))
        {
            temp = new Label(builder.toString(), getLabel(builder.toString()).getIndex());
        }
        else
        {
            temp = new Label(builder.toString(), ++indexLabel);
            labelTable.add(temp);
        }
        addLabelLine(builder.toString());
        addToken(temp);
    }

    private void nextChar()
    {
        builder.append(tmp);
        currentCharIndex++;
        tmp = code.charAt(currentCharIndex);
    }

    private boolean hasNextChar()
    {
        if (code.length() > currentCharIndex + 1)
        {
            return true;
        }
        return false;
    }

    public void clearBuilder()
    {
        if (builder.length() > 0)
        {
            builder.delete(0, builder.length());
        }
    }

    public void addToken()
    {
        tokenTable.add(new Lexeme(currentId++, line, builder.toString(), keywords.get(builder.toString()), null ));
    }

    public void addToken(Identifier ident)
    {
        tokenTable.add(new Lexeme(currentId++, line, builder.toString(), LexemeType.IDENT.getValue(), ident.getIndex()));
    }

    public void addToken(Constant constant)
    {
        tokenTable.add(new Lexeme(currentId++, line, builder.toString(), LexemeType.CONST.getValue(), constant.getIndex()));
    }

    public void addToken(Label label)
    {
        tokenTable.add(new Lexeme(currentId++, line, builder.toString(), LexemeType.LABEL.getValue(), label.getIndex()));
    }

    public Identifier getIdentifier(String identifier)
    {
        for (Identifier tmp : identTable) {
            if (tmp.getIdentifier().equals(identifier))
            {
                return tmp;
            }
        }
        return null;
    }

    public Constant getConstant(String constant)
    {
        for (Constant tmp : constTable) {
            if (tmp.getConstant().equals(constant))
            {
                return tmp;
            }
        }
        return null;
    }

    public Label getLabel(String label)
    {
        for (Label tmp : labelTable) {
            if (tmp.getLabel().equals(label))
            {
                return tmp;
            }
        }
        return null;
    }

    public boolean isExistIdentifier(String identifier)
    {
        for (Identifier tmp : identTable) {
            if (tmp.getIdentifier().equals(identifier))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isExistConstant(String constant)
    {
        for (Constant tmp : constTable) {
            if (tmp.getConstant().equals(constant))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isExistLabel(String label)
    {
        for (Label tmp : labelTable) {
            if (tmp.getLabel().equals(label))
            {
                return true;
            }
        }
        return false;
    }

    private void addLabelLine(String label) {
        for (Label tmp : labelTable) {
            if (tmp.getLabel().equals(label))
            {
                if (tokenTable.get(tokenTable.size()-1).getText().equals("goto"))
                {
                    if (tmp.getLineFrom() == -1)
                    {
                        tmp.setLineFrom(line);
                    }
                    else
                    {
                        lexicalErrors.add(new LabelRepeatedCallError(line, label));
                    }
                }
                else
                {
                    if (tmp.getLineTo() == -1)
                    {
                        tmp.setLineTo(line);
                    }
                    else
                    {
                        lexicalErrors.add(new LabelReDeclarationError(line, label));
                    }
                }

            }
        }
    }

    private boolean isDeclaration()
    {
        for (Lexeme tmp : tokenTable)
        {
            if (tmp.getLine() == line)
            {
                if (tmp.getCode() == IdentifierType.INT.getValue())
                {
                    lastType = tmp.getText();
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        return false;
    }

    private void checkLabelErrors() {
        for (Label tmp : labelTable)
        {
            if (tmp.getLineTo() == -1)
            {
                lexicalErrors.add(new LabelNotDeclaratedError(-1, tmp.getLabel()));
            }
        }
    }

    public static int getLexemeTypeIndex(String typeName)
    {
        return  keywords.containsKey(typeName) ? keywords.get(typeName) : -1;
    }
}