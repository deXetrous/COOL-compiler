package cool;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Arrays;
import java.lang.StringBuilder;
import java.util.Collections;

public class InheritanceGraph {

    /* Member variables */

    // List of nodes in the graph
    private List<Node> graph;

    private static AST.class_ ROOT_AST_CLASS = new AST.class_(Global.Constants.ROOT_TYPE, null, null, new ArrayList<>(), 0);
    private static Node ROOT_AST_NODE;
    
    // This map is used to find the index of particular class
    // in graph list
    private Map<String,Integer> classNameToIndexMap;

    public InheritanceGraph() {
        graph = new ArrayList<>();
        classNameToIndexMap = new HashMap<>();
    }

    // Adds the root class Object into the graph
    private void addObject() {
        // methods of Object
        ROOT_AST_NODE = new Node(ROOT_AST_CLASS, graph.size());
        ROOT_AST_CLASS.features = new ArrayList<>();
        ROOT_AST_CLASS.features.add(new AST.method("abort", new ArrayList<>(), Global.Constants.ROOT_TYPE, null, 0));
        ROOT_AST_CLASS.features.add(new AST.method("type_name", new ArrayList<>(), Global.Constants.STRING_TYPE, null, 0));
        ROOT_AST_CLASS.features.add(new AST.method("copy", new ArrayList<>(), Global.Constants.ROOT_TYPE, null, 0));

        classNameToIndexMap.put(Global.Constants.ROOT_TYPE, graph.size());
        graph.add(ROOT_AST_NODE);
    }

    private void addInt() {
        AST.class_ intAstClass = new AST.class_(Global.Constants.INT_TYPE, null, Global.Constants.ROOT_TYPE, new ArrayList<>(), 0);
        Node intNode = new Node(intAstClass, 0);

        classNameToIndexMap.put(Global.Constants.INT_TYPE, graph.size());
        graph.add(intNode);
    }

    private void addBool() {
        AST.class_ boolAstClass = new AST.class_(Global.Constants.BOOL_TYPE, null, Global.Constants.ROOT_TYPE, new ArrayList<>(), 0);
        Node boolNode = new Node(boolAstClass, 0);

        classNameToIndexMap.put(Global.Constants.BOOL_TYPE, graph.size());
        graph.add(boolNode);
    }

    // Adds the base class IO into the graph
    private void addIO() {

        List<AST.formal> stringFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.STRING_TYPE, 0)));

        List<AST.feature> ioFeatures = new ArrayList<>();
        List<AST.formal> intFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.INT_TYPE, 0)));

        ioFeatures.add(new AST.method("out_string", stringFormalList, Global.Constants.IO_TYPE, null, 0));
        ioFeatures.add(new AST.method("out_int", intFormalList, Global.Constants.IO_TYPE, null, 0));
        ioFeatures.add(new AST.method("in_string", new ArrayList<>(), Global.Constants.STRING_TYPE, null, 0));
        ioFeatures.add(new AST.method("in_int", new ArrayList<>(), Global.Constants.INT_TYPE, null, 0));

        AST.class_ ioAstClass = new AST.class_(Global.Constants.IO_TYPE, null, Global.Constants.ROOT_TYPE, ioFeatures, 0);
        Node ioNode = new Node(ioAstClass, 0);

        classNameToIndexMap.put(Global.Constants.IO_TYPE, graph.size());
        graph.add(ioNode);
    }

    // Adds the base class String into the graph
    private void addString() {
        List<AST.formal> stringFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.STRING_TYPE, 0)));

        List<AST.formal> intFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.INT_TYPE, 0)
            ,new AST.formal("y", Global.Constants.INT_TYPE, 0)));
        List<AST.feature> stringFeatures = new ArrayList<>();

        stringFeatures.add(new AST.method("length", new ArrayList<>(), Global.Constants.INT_TYPE, null, 0));
        stringFeatures.add(new AST.method("concat", stringFormalList, Global.Constants.STRING_TYPE, null, 0));
        stringFeatures.add(new AST.method("substr", intFormalList, Global.Constants.STRING_TYPE, null, 0));

        AST.class_ stringAstClass = new AST.class_(Global.Constants.STRING_TYPE, null, Global.Constants.ROOT_TYPE, stringFeatures, 0);
        Node stringNode = new Node(stringAstClass, 0);

        classNameToIndexMap.put(Global.Constants.STRING_TYPE, graph.size());
        graph.add(stringNode);
        
    }

    /* Method definitions */

    public Node getRootNode() {
        return ROOT_AST_NODE;
    }

    public boolean hasClass(String className) {
        return classNameToIndexMap.containsKey(className);
    }

    public List<Node> getNodeList() {
        return graph;
    }

    // * Returns the name of parent class of the given class name
    // * The existance of the passed class name in the graph
    //   has to be checked manually using 'hasClass' before
    //   calling this function
    public String getParentClassName(String className) {
        Node classNode = graph.get(classNameToIndexMap.get(className));
        return classNode.getAstClass().parent;
    }

    // Used to add a class to the graph
    public void addClass(AST.class_ astClass) {
        classNameToIndexMap.put(astClass.name, graph.size());
        graph.add(new Node(astClass, graph.size()));
    }

    public void update() {
        addObject();
        addIO();
        addString();
        addInt();
        addBool();
        updateParents();
    }

    // Updates the links between parents and child
    private void updateParents() {
        for(Node cl: graph) {
            if(cl.getAstClass().parent!=null) {
                // node has a parent
                // System.out.println(cl.getAstClass().parent);
                int parentIndex = classNameToIndexMap.get(cl.getAstClass().parent);
                cl.setParent(graph.get(parentIndex));
                graph.get(parentIndex).addChild(cl);
            }
        }
    }

    // gives join of type1 and type2 : nearest common parent
    public String getJoinOf(String type1, String type2) {
        if(type1.equals(type2)) {
            return type1;
        } else if(Utils.isPrimitiveType(type1) || Utils.isPrimitiveType(type2)) {
            return Global.Constants.ROOT_TYPE;
        }
        Node type1Node = graph.get(classNameToIndexMap.get(type1));
        Node type2Node = graph.get(classNameToIndexMap.get(type2));
        Node lca = getLCA(type1Node, type2Node);
        return lca.getAstClass().name;
    }

    // least common parent of node1 and node2
    private Node getLCA(Node node1, Node node2) {
        Node lca = null;
        List<Boolean> visited = new ArrayList<>(graph.size());
        visited.addAll(Collections.nCopies(graph.size(),Boolean.FALSE));
        while(node1!=null) {
            visited.set(node1.getIndex(),true);
            node1 = node1.getParent();
        }
        while(lca==null && node2!=null) {
            if(visited.get(node2.getIndex())) {
                lca = node2;
            }
            node2 = node2.getParent();
        }
        return lca;
    }

    // The basic node in the graph
    public static class Node {

        public static final int NO_PARENT = -1;

        private AST.class_ astClass;
        private int index;
        private Node parent;
        private List<Node> children;
        private boolean isInitiated;

        public Node(AST.class_ astClass, int index) {
            this.isInitiated = false;
            init(astClass, index);
        }

        private void init(AST.class_ astClass, int index) {
            if(isInitiated) return;
            this.astClass = astClass;
            this.index = index;
            this.children = new ArrayList<>();
            this.parent = null;
            this.isInitiated = true;
        }

        public void addChild(Node child) {
            children.add(child);
        }

        public AST.class_ getAstClass() {
            return astClass;
        }

        public int getIndex() {
            return index;
        }

        public boolean parentExists() {
            return parent!=null;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public List<Node> getChildren() {
            return children;
        }
        
        public boolean equals(Node node) {
            return this.index == node.getIndex();
        }
    }
}
