/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index.outlook;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.DocumentType;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeIndex;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexWriterAdapter;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import com.pff.PSTObject;

/**
 * @author Tran Nam Quang
 */
public final class OutlookIndex extends TreeIndex<MailDocument, MailFolder> {
	
	/*
	 * Tests indicate we can directly retrieve any message via its ID, but not
	 * attachments. This means in order to retrieve an attachment, we'll have to
	 * fetch its message first.
	 * 
	 * Tests also indicate that nothing serious happens if we read a PST file
	 * that is currently open in Outlook. Since we're only reading the PST file,
	 * the worst that could happen is to get some garbage out of the PST file,
	 * as a result of Outlook writing to it concurrently.
	 */
	
	private static final long serialVersionUID = 1L;
	
	private MailFolder simplifiedRootFolder;
	
	public OutlookIndex(@Nullable File indexParentDir, @NotNull File pstFile) {
		super(indexParentDir, pstFile);
	}
	
	@NotNull
	protected String getIndexDirName(@NotNull File pstFile) {
		return Util.splitFilename(pstFile)[0];
	}
	
	@NotNull
	protected MailFolder createRootFolder(@NotNull Path path) {
		return new MailFolder(path);
	}
	
	public boolean isEmailIndex() {
		return true;
	}
	
	public DocumentType getDocumentType() {
		return DocumentType.OUTLOOK;
	}
	
	public IndexingResult doUpdate(	@NotNull IndexingReporter reporter,
									@NotNull Cancelable cancelable) {
		reporter.setStartTime(System.currentTimeMillis());
		MailFolder rootFolder = getRootFolder();
		rootFolder.setError(null);
		IndexWriterAdapter writer = null;
		
		try {
			/*
			 * Return immediately if the last-modified field of the PST file
			 * hasn't changed.
			 */
			File rootFile = getCanonicalRootFile();
			long newLastModified = rootFile.lastModified();
			if (UtilModel.isUnmodifiedArchive(rootFolder, newLastModified))
				return IndexingResult.SUCCESS_UNCHANGED;
			rootFolder.setLastModified(newLastModified);
			
			writer = new IndexWriterAdapter(getLuceneDir());
			OutlookContext context = new OutlookContext(
					getConfig(), writer, reporter, cancelable
			);
			PSTFile pstFile = new PSTFile(rootFile.getPath());
			visitFolder(context, rootFolder, pstFile.getRootFolder());
			
			simplifiedRootFolder = new TreeRootSimplifier<MailFolder> () {
				protected boolean hasContent(MailFolder node) {
					return node.getDocumentCount() > 0;
				}
				protected boolean hasDeepContent(MailFolder node) {
					return node.hasDeepContent();
				}
				protected Collection<MailFolder> getChildren(MailFolder node) {
					return node.getSubFolders();
				}
			}.getSimplifiedRoot(getRootFolder());
			
			writer.optimize();
			return IndexingResult.SUCCESS_CHANGED;
		}
		catch (PSTException e) {
			report(reporter, e);
		}
		catch (IOException e) {
			report(reporter, e);
		}
		catch (IndexingException e) {
			report(reporter, e.getIOException());
		}
		finally {
			Closeables.closeQuietly(writer);
			reporter.setEndTime(System.currentTimeMillis());
		}
		return IndexingResult.FAILURE;
	}
	
	private void report(@NotNull IndexingReporter reporter,
						@NotNull Exception e) {
		MailFolder rootFolder = getRootFolder();
		IndexingError error = new IndexingError(
			ErrorType.IO_EXCEPTION, rootFolder, e);
		rootFolder.setError(error);
		reporter.fail(error);
	}
	
	// TODO post-release-1.1: method currently not in use
	@NotNull
	public MailFolder getSimplifiedRootFolder() {
		if (simplifiedRootFolder == null) // update method hasn't been called yet
			return getRootFolder();
		return simplifiedRootFolder;
	}
	
	// TODO doc: stores in the given folder whether it has 'deep' content or not.
	@RecursiveMethod
	private static void visitFolder(@NotNull OutlookContext context,
									@NotNull MailFolder folder,
									@NotNull PSTFolder pstFolder)
			throws IndexingException, PSTException {
		folder.setHasDeepContent(false);
		final Map<String, MailDocument> unseenMails = Maps.newHashMap(folder.getDocumentMap());
		final Map<String, MailFolder> unseenSubFolders = Maps.newHashMap(folder.getSubFolderMap());
		final List<PSTFolder> subFoldersToVisit = new LinkedList<PSTFolder>();
		
		// Visit mails
		if (pstFolder.getContentCount() > 0) {
			try {
				PSTObject pstObject;
				try {
					pstObject = pstFolder.getNextChild();
				}
				catch (IndexOutOfBoundsException e) {
					// Bug #374. See similar bugfix inside the following loop.
					Util.printErr(e.getMessage());
					pstObject = null; // skip following loop
				}
				
				while (pstObject != null) {
					if (context.isStopped()) break;
					
					/*
					 * Bug #3561223: The documentation for java-libpst 0.7
					 * indicates we can expect the PST object to be an instance
					 * of PSTMessage. However, a bug report has shown that it
					 * may also be a PSTFolder, probably in some very rare
					 * cases.
					 */
					if (pstObject instanceof PSTFolder) {
						subFoldersToVisit.add((PSTFolder) pstObject);
					}
					else if (pstObject instanceof PSTMessage) {
						/*
						 * Note: The user should not be allowed to stop the
						 * indexing in the middle of email processing (e.g.
						 * between attachments), otherwise we could end up
						 * indexing emails which have only one half of all
						 * attachments, which would complicate email
						 * modification detection.
						 */
						/*
						 * Note: For the email UID, we'll use the 'descriptor
						 * node ID' rather than the 'internet message ID', for
						 * several reasons: (1) Not every email has an internet
						 * message ID, e.g. unsent emails. (2) The descriptor
						 * node ID is only an internal ID used by Outlook, but
						 * it does not change. (3) The descriptor node ID allows
						 * fast retrieval of single emails, which is what we
						 * need for the preview.
						 */
						PSTMessage pstMail = (PSTMessage) pstObject;
						String id = String.valueOf(pstMail.getDescriptorNodeId());
						Date newLastModDate = pstMail.getLastModificationTime();
						// Bug #397: The last-modification date can be null
						long newLastMod = newLastModDate == null ? 0 : newLastModDate.getTime();
						MailDocument mail = unseenMails.remove(id);
						if (mail == null) { // Mail added
							String subject = pstMail.getSubject();
							mail = new MailDocument(
								folder, id, subject, newLastMod);
							context.index(mail, pstMail, true);
						}
						else if (mail.isModified(newLastMod)) { // Mail modified
							/*
							 * Note: Outlook mails are not immutable, because
							 * Outlook allows modifying the subject and body, as
							 * well as removing attachments. Such modifications
							 * will alter the last-modified value as provided by
							 * the PSTMessage object. It is not clear though
							 * whether any other changes are possible, and if
							 * so, whether we can rely on the last-modified
							 * value to reflect such changes.
							 */
							mail.setLastModified(newLastMod);
							context.index(mail, pstMail, false);
						}
					}
					
					try {
						pstObject = pstFolder.getNextChild();
					}
					catch (IndexOutOfBoundsException e) {
						/*
						 * Temporary fix for bug #3489947. Affects java-libpst
						 * v0.5 and probably also v0.7.
						 */
						Util.printErr(e.getMessage());
						pstObject = null; // get out of loop
					}
				}
			} catch (IOException e) {
				throw new IndexingException(e);
			}
			folder.setHasDeepContent(true);
		}
		
		// Visit subfolders
		if (pstFolder.hasSubfolders() || !subFoldersToVisit.isEmpty()) {
			try {
				subFoldersToVisit.addAll(pstFolder.getSubFolders());
				for (PSTFolder pstSubFolder : subFoldersToVisit) {
					if (context.isStopped()) break;
					String foldername = pstSubFolder.getDisplayName();
					MailFolder subFolder = unseenSubFolders.remove(foldername);
					if (subFolder == null)
						subFolder = new MailFolder(folder, foldername);
					visitFolder(context, subFolder, pstSubFolder);
					if (subFolder.hasDeepContent())
						folder.setHasDeepContent(true);
				}
			} catch (IOException e) {
				throw new IndexingException(e);
			}
		}

		if (context.isStopped()) return;
		
		// Handle missing mails and folders
		for (MailDocument mail : unseenMails.values()) {
			/*
			 * Bug #3475969: The UID must be retrieved *before* detaching the
			 * document from its parent, as constructing the UID requires
			 * accessing the parent.
			 */
			context.deleteFromIndex(mail.getUniqueId());
			folder.removeDocument(mail);
		}
		for (MailFolder subFolder : unseenSubFolders.values())
			folder.removeSubFolder(subFolder);
	}

}
