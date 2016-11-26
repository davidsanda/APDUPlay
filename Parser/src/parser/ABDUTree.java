/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javafx.util.Pair;

/**
 *
 * @author Andrej
 */
public class ABDUTree {
    private int packetsCount;
    public final ABDUNode receivedRoot;
    public final ABDUNode root;
    public final short header;
    public final List<Pair<ABDUNode, ABDUNode>> streamPairs;
    
    private ABDUNode lastTransmittedNode;
    
    public ABDUTree(byte[] stream) {
        ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(stream, 0, 2));
        header = wrapped.getShort();
        root = new ABDUNode(wrapped.array());
        root.addChild(new ABDUNode(Arrays.copyOfRange(stream, 2, stream.length)));
        receivedRoot = new ABDUNode(wrapped.array());
        streamPairs = new LinkedList<>();
        init();
    }
    
    public ABDUTree(byte[] header, byte[] data) {
        ByteBuffer wrapped = ByteBuffer.wrap(header);
        this.header = wrapped.getShort();
        root = new ABDUNode(wrapped.array());
        root.addChild(new ABDUNode(data));
        receivedRoot = new ABDUNode(wrapped.array());
        streamPairs = new LinkedList<>();
        init();
    }
    
    public void merge(byte[] stream) {
        lastTransmittedNode = merge(root, stream);
        packetsCount++;
    }
    
    public void addReceivedData(byte[] data) {
        ABDUNode node = merge(receivedRoot, data);
        if (node != null && lastTransmittedNode != null) {
            streamPairs.add(new Pair(lastTransmittedNode, node));
        }
    }
    
    public int getPacketsCount() {
        return packetsCount;
    }
    
    public void simplifyNodes() {
        simplifyNodes(root);
        simplifyNodes(receivedRoot);
    }
    
    private void init() {
        receivedRoot.setCount(0);
        packetsCount = 1;
        
        // must have a child
        lastTransmittedNode = root.getChildNodes().iterator().next();
    }
    
    private void simplifyNodes(ABDUNode node) {
        Queue<ABDUNode> queue = new ArrayDeque<>();
        queue.add(node);
        while(!queue.isEmpty()) {
            node = queue.remove();
            Collection<ABDUNode> childNodes = node.getChildNodes();
            byte[] data = node.getData();
            if (data.length > 1) {
                ABDUNode firstNode = new ABDUNode(Arrays.copyOfRange(data, 0, 1));
                ABDUNode lastNode = firstNode;
                for (int i = 1; i < data.length; i++) {
                    ABDUNode n = new ABDUNode(Arrays.copyOfRange(data, i, i + 1));
                    lastNode.setCount(node.getCount());
                    lastNode.addChild(n);
                    lastNode = n;
                }
                
                lastNode.setCount(node.getCount());
                lastNode.addChildren(childNodes);
                node.setData(firstNode.getData());
                node.setChildren(firstNode.getChildNodes());
            }
            
            queue.addAll(childNodes);
        }
    }
    
    private ABDUNode merge(ABDUNode node, byte[] stream) {
        node.incrementCount();
        
        ABDUNode currentNode = node.findChildNode(stream[0]);
        if (currentNode == null) {
            currentNode = new ABDUNode(stream);
            node.addChild(currentNode);
            return currentNode;
        }
        
        byte[] data = currentNode.getData();
        int currentIndex = 0;
        ABDUNode lastNode = null;
        for (int i = 0; i < stream.length; i++) {
            if (currentIndex >= data.length) {
                lastNode = currentNode.findChildNode(stream[i]);
                if (lastNode == null) {
                    lastNode = new ABDUNode(Arrays.copyOfRange(stream, i, stream.length));
                    currentNode.addChild(lastNode);
                    break;
                }
                
                currentNode.incrementCount();
                currentIndex = 0;
                currentNode = lastNode;
                data = currentNode.getData();
            }
            
            if (data[currentIndex] != stream[i]) {
                if (currentIndex != 0) {
                    currentNode.divide(currentIndex);
                }
                lastNode = new ABDUNode(Arrays.copyOfRange(stream, i, stream.length));
                currentNode.addChild(lastNode);
                break;
            }
            
            currentIndex++;
        }
        
        currentNode.incrementCount();
        return lastNode;
    }
}
