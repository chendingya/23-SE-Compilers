package callgraph;

import cymbol.CymbolBaseListener;
import cymbol.CymbolParser;

public class CallGraphListener extends CymbolBaseListener {
  private Graph graph = new Graph();
  String currentFunctionName = null;

  @Override
  public void enterFunctionDecl(CymbolParser.FunctionDeclContext ctx) {
    currentFunctionName = ctx.ID().getText();
    graph.addNode(currentFunctionName);
  }

  @Override
  public void enterCall(CymbolParser.CallContext ctx) {
    String callee = ctx.ID().getText();
    graph.addEdge(currentFunctionName, callee);
  }

  public Graph getGraph() {
    return this.graph;
  }
}
