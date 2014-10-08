package com.avmurzin.avrora.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.avmurzin.avrora.ui.TreeElement;

/**
 * Элемент для формирования дерева каталогов (в JSON). Данные хранятся
 * в domain Container. Дерево формируется в singleton-е ContainerTree
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
public class TreeElement {
	private String id;
	private String value;
	private String image;
	private Collection<TreeElement> data;
	
	public TreeElement() {
		this.data = new ArrayList<TreeElement>();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Collection<TreeElement> getData() {
		return data;
	}
	public void setData(List<TreeElement> data) {
		this.data = data;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
}
