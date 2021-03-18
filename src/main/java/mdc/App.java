package mdc;

public class App {

    public static void main(String[] args) {
        Parser p = new Parser();

        p.parseTree("|;,\\|section1|section2|section3");
    }
}
