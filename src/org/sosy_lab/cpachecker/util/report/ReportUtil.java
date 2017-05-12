/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
 *
 * Open-source component:
 *
 * CPAchecker
 * Copyright (C) 2007-2014  Dirk Beyer
 *
 * Guava: Google Core Libraries for Java
 * Copyright (C) 2010-2006  Google
 *
 *
 */
package org.sosy_lab.cpachecker.util.report;

import static com.google.common.collect.Iterables.getLast;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReportWithTrace;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

import java.util.ArrayList;
import java.util.List;

import cn.edu.thu.tsmart.tool.bd.report.Coordinate;
import cn.edu.thu.tsmart.tool.bd.report.FaultResult;
import cn.edu.thu.tsmart.tool.bd.report.orientation.Area;
import cn.edu.thu.tsmart.tool.bd.report.orientation.Position;
import cn.edu.thu.tsmart.tool.bd.report.section.AbstractSection;
import cn.edu.thu.tsmart.tool.bd.report.section.Location;
import cn.edu.thu.tsmart.tool.bd.report.section.TransferRelation;
import cn.edu.thu.tsmart.tool.bd.report.supplementation.ISupplementation;

public class ReportUtil {

  public static FaultResult convert(ErrorReport error) {
    // TODO get the parameters from ErrorReport, now just use extremely simple data
    FaultResult.Severity severity = FaultResult.Severity.ERROR;
    FaultResult.Confidence confidence = FaultResult.Confidence.MAY;
    String weakness = error.getWeakness().getWeaknessName();

    if (error instanceof ErrorReportWithTrace) {
      ARGPath path = ((ErrorReportWithTrace) error).getErrorTrace();
      return convertFromTrace(severity, confidence, weakness, path);
    } else {
      ErrorSpot errorSpot = error.getErrorSpot();
      return convertFromErrorSpot(severity, confidence, weakness, errorSpot);
    }

  }

  public static FaultResult convertFromErrorSpot(
      FaultResult.Severity severity,
      FaultResult.Confidence confidence, String weakness, ErrorSpot errorSpot) {
    if (errorSpot.getCFAEdge().isPresent()) {
      CFAEdge edge = errorSpot.getCFAEdge().get();
      AbstractSection transfer = convertEdge(edge);
      List<AbstractSection> list = new ArrayList<>();
      if (transfer != null) {
        list.add(transfer);
      }

      return new FaultResult(severity, confidence, weakness, list);
    } else {
      CAstNode astNode = errorSpot.getASTNode().get();
      FileLocation fileLocation = astNode.getFileLocation();

      String file = fileLocation.getFileName();
      // TODO: get the proper function name
      String function = "No function name";
      int startLineNumber = fileLocation.getStartingLineNumber();
      int startOffset = fileLocation.getNodeOffset();
      int endLineNumber = fileLocation.getEndingLineNumber();
      int endOffset = fileLocation.getNodeOffset() + fileLocation.getNodeLength();

      Area area = new Area(file, function, new Coordinate(startLineNumber, startOffset),
          new Coordinate(endLineNumber, endOffset));
      String code = errorSpot.getCode();
      List<ISupplementation> supplementation = new ArrayList<>();

      TransferRelation transfer = new TransferRelation(area, code, supplementation);
      List<AbstractSection> list = new ArrayList<>();
      list.add(transfer);

      return new FaultResult(severity, confidence, weakness, list);
    }
  }

  public static FaultResult convertFromTrace(
      FaultResult.Severity severity,
      FaultResult.Confidence confidence, String weakness, ARGPath path) {
    // create the abstractSection list in current faultResult
    List<AbstractSection> list = new ArrayList<>();

    PathIterator it = path.pathIterator();
    while (it.hasNext()) {
      ARGState ss = it.getAbstractState();
      AbstractSection location = convertState(it.getAbstractState());
      if (location != null) {
        list.add(location);
      }
      if (it.hasNext()) {
        AbstractSection transfer = convertEdge(it.getOutgoingEdge());
        if (transfer != null) {
          list.add(transfer);
        }
      }
      it.advance();
    }

    FaultResult state = new FaultResult(severity, confidence, weakness, list);
    return state;
  }

  public static AbstractSection convertState(ARGState state) {
    // NOTICE:
    // the child structure of ARGState should be compositeState
    // and the compositeState should include locationState
    AbstractState wrappedState = state.getWrappedState();
    if (wrappedState instanceof CompositeState) {
      for (AbstractState abstractState : ((CompositeState) wrappedState).getWrappedStates()) {
        if (abstractState instanceof LocationState) {
          // get CFANode and CFAEdge through LocationState
          CFANode node = ((LocationState) abstractState).getLocationNode();

          if (node.getNumLeavingEdges() > 0) {
            CFAEdge edge = node.getLeavingEdge(0);
            if (edge instanceof MultiEdge) {
              edge = ((MultiEdge) edge).getEdges().get(0);
            }

            FileLocation fileLocation = edge.getFileLocation();
            String file = fileLocation.getFileName();
            String function = getFunctionName(edge);
            int lineNumber = fileLocation.getStartingLineNumber();
            int offset = fileLocation.getNodeOffset();

            Position position =
                new Position(file, function, new Coordinate(lineNumber, offset));
            List<ISupplementation> supplementation = new ArrayList<>();

            Location location = new Location(position, supplementation);
            return location;

          } else {
            if (node.getNumEnteringEdges() > 0) {
              CFAEdge edge = node.getEnteringEdge(0);
              if (edge instanceof MultiEdge) {
                edge = getLast(((MultiEdge) edge).getEdges());
              }

              FileLocation fileLocation = edge.getFileLocation();
              String file = fileLocation.getFileName();
              String function = getFunctionName(edge);
              int lineNumber = fileLocation.getEndingLineNumber();
              int offset = fileLocation.getNodeOffset() + fileLocation.getNodeLength();

              Position position =
                  new Position(file, function, new Coordinate(lineNumber, offset));
              List<ISupplementation> supplementation = new ArrayList<>();

              Location location = new Location(position, supplementation);
              return location;
            }
          }
        }
      }
    }

    assert false;
    return null;
  }

  public static AbstractSection convertEdge(CFAEdge edge) {
    CFAEdge infoEdge;
    // change the edge to callToReturnEdge to represent raw Cstatement
    if (edge == null) {
      return null;
    }
    if (edge instanceof CFunctionReturnEdge) {
      infoEdge = ((CFunctionReturnEdge) edge).getSummaryEdge();
    } else {
      infoEdge = edge;
    }

    FileLocation fileLocation = infoEdge.getFileLocation();
    String file = fileLocation.getFileName();
    String function = getFunctionName(infoEdge);
    int startLineNumber = fileLocation.getStartingLineNumber();
    int startOffset = fileLocation.getNodeOffset();
    int endLineNumber = fileLocation.getEndingLineNumber();
    int endOffset = fileLocation.getNodeOffset() + fileLocation.getNodeLength();

    Area area = new Area(file, function, new Coordinate(startLineNumber, startOffset),
        new Coordinate(endLineNumber, endOffset));
    String code = infoEdge.getRawStatement();
    List<ISupplementation> supplementation = new ArrayList<>();

    TransferRelation transfer = new TransferRelation(area, code, supplementation);
    return transfer;
  }

  /**
   * Return function name of the CFAEdge
   */
  public static String getFunctionName(CFAEdge pEdge) {
    return pEdge.getPredecessor().getFunctionName();
  }


}
