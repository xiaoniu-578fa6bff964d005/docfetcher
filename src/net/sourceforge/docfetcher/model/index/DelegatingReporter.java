/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index;

import java.util.List;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.TreeNode;

/**
 * @author Tran Nam Quang
 */
public final class DelegatingReporter extends IndexingReporter {

	public interface ExistingMessagesHandler {
		public void handleMessages(	List<InfoMessage> infoMessages,
									List<ErrorMessage> errorMessages);
	}

	public interface ExistingMessagesProvider {
		public List<InfoMessage> getInfoMessages();

		public List<ErrorMessage> getErrorMessages();
	}

	public static final class InfoMessage {
		private final InfoType infoType;
		private final TreeNode treeNode;

		private InfoMessage(InfoType infoType, TreeNode treeNode) {
			this.infoType = infoType;
			this.treeNode = treeNode;
		}

		public InfoType getInfoType() {
			return infoType;
		}

		public TreeNode getTreeNode() {
			return treeNode;
		}
	}

	public static final class ErrorMessage {
		private final ErrorType errorType;
		private final TreeNode treeNode;
		private final Throwable cause;

		private ErrorMessage(	ErrorType errorType,
								TreeNode treeNode,
								Throwable cause) {
			this.errorType = errorType;
			this.treeNode = treeNode;
			this.cause = cause;
		}

		public ErrorType getErrorType() {
			return errorType;
		}

		public TreeNode getTreeNode() {
			return treeNode;
		}

		public Throwable getCause() {
			return cause;
		}
	}

	@Nullable
	private IndexingReporter delegate;
	private final BoundedList<InfoMessage> infoMessages;
	private final BoundedList<ErrorMessage> errorMessages;

	DelegatingReporter(int capacity) {
		infoMessages = new BoundedList<InfoMessage>(capacity);
		errorMessages = new BoundedList<ErrorMessage>(capacity);
	}

	public synchronized void attachDelegate(@NotNull IndexingReporter delegate,
											@NotNull ExistingMessagesHandler handler) {
		Util.checkNotNull(delegate, handler);
		Util.checkThat(this.delegate == null);
		this.delegate = delegate;
		handler.handleMessages(
			infoMessages.removeAll(), errorMessages.removeAll());
	}

	public synchronized void detachDelegate(@NotNull IndexingReporter delegate,
											@NotNull ExistingMessagesProvider provider) {
		Util.checkNotNull(delegate, provider);
		Util.checkThat(this.delegate == delegate);
		Util.checkThat(infoMessages.isEmpty() && errorMessages.isEmpty());
		this.delegate = null;
		infoMessages.addAll(provider.getInfoMessages());
		errorMessages.addAll(provider.getErrorMessages());
	}

	public synchronized void indexingStarted() {
		if (delegate != null)
			delegate.indexingStarted();
	}

	public synchronized void indexingStopped() {
		if (delegate != null)
			delegate.indexingStopped();
	}

	public synchronized void info(InfoType infoType, TreeNode treeNode) {
		if (delegate != null)
			delegate.info(infoType, treeNode);
		else
			infoMessages.add(new InfoMessage(infoType, treeNode));
	}

	public synchronized void fail(	ErrorType errorType,
									TreeNode treeNode,
									Throwable cause) {
		if (delegate != null)
			delegate.fail(errorType, treeNode, cause);
		else
			errorMessages.add(new ErrorMessage(errorType, treeNode, cause));
	}

}