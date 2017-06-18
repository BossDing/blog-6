package me.qyh.blog.core.vo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * Tree Node
 * 
 * @author mhlx
 *
 */
public class Node<T> {

	private String text;

	private List<T> nodes;

	public Node() {
		super();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<T> getNodes() {
		return nodes;
	}

	public void setNodes(List<T> nodes) {
		this.nodes = nodes;
	}

	public boolean hasChild() {
		return !CollectionUtils.isEmpty(nodes);
	}

	public void add(T t) {
		if (nodes == null) {
			nodes = new ArrayList<>();
		}
		nodes.add(t);
	}

}
