package mdc;

import java.util.List;
import java.util.ArrayList;

public class Node {
    private String value; 
    private List<Node> children;

    public Node(String value, List<Node> children) {
        this.value = value;
        this.children = children;
    }

    public Node getChild(List<Integer> location) {  
        if (children.size() == 0) {
            return null;
        } else if (location.size() != 1) {
            return this.children.get(location.remove(0)).getChild(location);
        } else {
            return this.children.get(location.remove(0));
        }
    }
}