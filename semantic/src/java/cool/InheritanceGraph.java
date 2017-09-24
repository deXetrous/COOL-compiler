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

    private static final int ROOT_CLASS_INDEX = 0;
    private static AST.class_ ROOT_AST_CLASS = new AST.class_(Global.Constants.ROOT_TYPE, null, null, new ArrayList<>(), 0);
    private static Node ROOT_AST_NODE = new Node(ROOT_AST_CLASS, ROOT_CLASS_INDEX);

    public static String MAIN_CLASS_NAME = "Main";

    public static Map<String,Integer> classNameToIndexMap;
    private List<Node> graph;
    private boolean hasMain;

    public InheritanceGraph() {
        graph = new ArrayList<>();
        classNameToIndexMap = new HashMap<>();
        hasMain = false;
        addBaseClasses();
    }

    public Node getRootNode() {
        return ROOT_AST_NODE;
    }

    public boolean hasMain() {
        return hasMain;
    }

    public boolean hasClass(String className) {
        return classNameToIndexMap.containsKey(className);
    }

    public List<Node> getNodeList() {
        return graph;
    }

    public String getParentClassName(String className) {
        Node classNode = graph.get(classNameToIndexMap.get(className));
        return classNode.getAstClass().parent;
    }

    public void addClass(AST.class_ astClass) {
        if(classNameToIndexMap.containsKey(astClass.name)) {
            Global.errorReporter.report(Global.filename, astClass.getLineNo(),
                new StringBuilder().append("class '").append(astClass.name)
                    .append("' has been redefined").toString());
        } else if(isRestrictedClass(astClass.name)) {
            Global.errorReporter.report(Global.filename, astClass.getLineNo(),
                new StringBuilder().append("Cannot redefine base class '")
                    .append(astClass.name).append("'").toString());
        } else {
            classNameToIndexMap.put(astClass.name, graph.size());
            graph.add(new Node(astClass, graph.size()));
            if(MAIN_CLASS_NAME.equals(astClass.name)) {
                hasMain = true;
            }
        }
    }

    public boolean isConforming(String type1, String type2) {
        if(type1.equals(type2) || Global.Constants.ROOT_TYPE.equals(type1)) {
            return true;
        } else if(isRestrictedInheritanceClass(type1) || isRestrictedInheritanceClass(type2)) {
            return false;
        }
        Node type1Node = graph.get(classNameToIndexMap.get(type1));
        Node type2Node = graph.get(classNameToIndexMap.get(type2));
        while(type2Node.parentExists()) {
            type2Node = type2Node.getParent();
            if(type1Node.equals(type2Node)) {
                return true;
            }
        }
        return false;
    }
    
    public String getJoinOf(String type1, String type2) {
        if(type1.equals(type2)) {
            return type1;
        } else if(isRestrictedInheritanceClass(type1) || isRestrictedInheritanceClass(type2)) {
            return Global.Constants.ROOT_TYPE;
        }
        Node type1Node = graph.get(classNameToIndexMap.get(type1));
        Node type2Node = graph.get(classNameToIndexMap.get(type2));
        Node lca = getLCA(type1Node, type2Node);
        return lca.getAstClass().name;
    }

    public boolean analyze() {
        boolean hasError = false;
        updateParents();

        if(!hasMain()) {
            hasError = true;
            Global.errorReporter.report(Global.filename, 0,"'Main' class is missing.");
        }

        List<Stack<Node>> cycles = getCyclesInGraph();
        if(!cycles.isEmpty()) {
            hasError = true;
            StringBuilder errorString = new StringBuilder();
            StringBuilder cyclePath = new StringBuilder();
            for(Stack<Node> cycle: cycles) {
                cyclePath.setLength(0);
                int size = cycle.size();
                for(int i=0; i<size-1; i++) {
                    cyclePath.append(cycle.pop().getAstClass().name).append(" -> ");
                }

                AST.class_ lastClass = cycle.pop().getAstClass();
                String lastClassName = lastClass.name;
                
                errorString.setLength(0);
                errorString.append("Classes have cyclic dependency: ");
                errorString.append(lastClassName).append(" -> ");
                errorString.append(cyclePath).append(lastClassName);
                Global.errorReporter.report(Global.filename, lastClass.getLineNo(), errorString.toString());
            }
        }
        return hasError;
    }


    private void addBaseClasses() {
        addObject();
        addIO();
        addString();
        classNameToIndexMap.put(Global.Constants.INT_TYPE, -1);
        classNameToIndexMap.put(Global.Constants.BOOL_TYPE, -1);

    }

    private void addObject() {
        ROOT_AST_CLASS.features = new ArrayList<>();
        ROOT_AST_CLASS.features.add(new AST.method("abort", new ArrayList<>(), Global.Constants.ROOT_TYPE, null, 0));
        ROOT_AST_CLASS.features.add(new AST.method("type_name", new ArrayList<>(), Global.Constants.STRING_TYPE, null, 0));
        ROOT_AST_CLASS.features.add(new AST.method("copy", new ArrayList<>(), Global.Constants.ROOT_TYPE, null, 0));

        classNameToIndexMap.put(Global.Constants.ROOT_TYPE, ROOT_CLASS_INDEX);
        graph.add(ROOT_AST_NODE);
    }

    private void addIO() {
        List<AST.formal> stringFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.STRING_TYPE, 0)));

        List<AST.feature> ioFeatures = new ArrayList<>();
        List<AST.formal> intFormalList1 = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.INT_TYPE, 0)));

        ioFeatures.add(new AST.method("out_string", stringFormalList, Global.Constants.IO_TYPE, null, 0));
        ioFeatures.add(new AST.method("out_int", intFormalList1, Global.Constants.IO_TYPE, null, 0));
        ioFeatures.add(new AST.method("in_string", new ArrayList<>(), Global.Constants.STRING_TYPE, null, 0));
        ioFeatures.add(new AST.method("in_int", new ArrayList<>(), Global.Constants.INT_TYPE, null, 0));

        AST.class_ ioAstClass = new AST.class_(Global.Constants.IO_TYPE, null, Global.Constants.ROOT_TYPE, ioFeatures, 0);
        Node ioNode = new Node(ioAstClass, 0);

        classNameToIndexMap.put(Global.Constants.IO_TYPE, graph.size());
        graph.add(ioNode);
    }

    private void addString() {
        List<AST.formal> stringFormalList = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.STRING_TYPE, 0)));

        List<AST.formal> intFormalList2 = new ArrayList<>(Arrays.asList(new AST.formal("x", Global.Constants.INT_TYPE, 0)
            ,new AST.formal("y", Global.Constants.INT_TYPE, 0)));
        List<AST.feature> stringFeatures = new ArrayList<>();

        stringFeatures.add(new AST.method("length", new ArrayList<>(), Global.Constants.INT_TYPE, null, 0));
        stringFeatures.add(new AST.method("concat", stringFormalList, Global.Constants.STRING_TYPE, null, 0));
        stringFeatures.add(new AST.method("substr", intFormalList2, Global.Constants.STRING_TYPE, null, 0));

        AST.class_ stringAstClass = new AST.class_(Global.Constants.STRING_TYPE, null, Global.Constants.ROOT_TYPE, stringFeatures, 0);
        Node stringNode = new Node(stringAstClass, 0);

        classNameToIndexMap.put(Global.Constants.STRING_TYPE, graph.size());
        graph.add(stringNode);
        
    }

    private boolean isRestrictedClass(String name) {
        return Global.Constants.IO_TYPE.equals(name) || Global.Constants.INT_TYPE.equals(name) 
        || Global.Constants.STRING_TYPE.equals(name) || Global.Constants.BOOL_TYPE.equals(name);
    }

    private boolean isRestrictedInheritanceClass(String name) {
        return Global.Constants.INT_TYPE.equals(name) || Global.Constants.STRING_TYPE.equals(name) 
        || Global.Constants.BOOL_TYPE.equals(name);
    }

    public boolean isNoMethodClass(String name) {
        return Global.Constants.INT_TYPE.equals(name) || Global.Constants.BOOL_TYPE.equals(name);
    }

    private void updateParents() {
        for(Node cl: graph) {
            if(cl.getAstClass().parent!=null) {
                if(isRestrictedInheritanceClass(cl.getAstClass().parent)) {
                    Global.errorReporter.report(Global.filename, cl.getAstClass().getLineNo(), 
                                new StringBuilder().append("Cannot inherit base class '").append(cl.getAstClass().parent)
                                .append("'").toString());
                } else if(classNameToIndexMap.containsKey(cl.getAstClass().parent)) {
                    int parentIndex = classNameToIndexMap.get(cl.getAstClass().parent);
                    cl.setParent(graph.get(parentIndex));
                    graph.get(parentIndex).addChild(cl);
                } else {
                    Global.errorReporter.report(Global.filename, cl.getAstClass().getLineNo(), 
                                new StringBuilder().append("Inherited class '").append(cl.getAstClass().parent)
                                .append("' for '").append(cl.getAstClass().name).append("' has not been declared").toString());
                }
            } else {
                if(!Global.Constants.ROOT_TYPE.equals(cl.getAstClass().name)) {
                    cl.setParent(ROOT_AST_NODE);
                    ROOT_AST_NODE.addChild(cl);
                }
            }
        }
    }

    private boolean getCyclesInGraphUtil(int v, List<Boolean> visited, List<Boolean> recStack, Stack<Node> cycle) {
        Node currentNode = graph.get(v);
        cycle.push(currentNode);
        if(visited.get(v) == false) {
            visited.set(v, true);
            recStack.set(v, true);
            if(currentNode.parentExists()) {
                int parentIndex = currentNode.getParent().getIndex();
                if(parentIndex != Node.NO_PARENT) {
                    if ( (!visited.get(parentIndex) && getCyclesInGraphUtil(parentIndex, visited, recStack, cycle)) 
                          || recStack.get(parentIndex) ) {
                        return true;
                    }
                }
            }
        }
        cycle.pop();
        recStack.set(v, false);
        return false;
    }

    private List<Stack<Node>> getCyclesInGraph() {

        int V = graph.size();
        List<Boolean> visited = new ArrayList<>();
        List<Boolean> recStack = new ArrayList<>();
        Stack<Node> cycle = new Stack<>();
        for(int i = 0; i < V; i++) {
            visited.add(false);
            recStack.add(false);
        }
         
         List<Stack<Node>> cycles = new ArrayList<>();
        for(int i = 0; i < V; i++)
            if (getCyclesInGraphUtil(i, visited, recStack, cycle)) {
                cycles.add(cycle);
                cycle = new Stack<>();
            }
     
        return cycles;
    }
    
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
