package com.gmmapowell.vc;

import java.io.File;
import java.util.List;

import com.gmmapowell.git.GitRecord;
import com.gmmapowell.utils.OrderedFileList;

public interface VCHelper {
	void removeNonManagedFiles(OrderedFileList files);
	GitRecord checkFiles(boolean doComparison, OrderedFileList files, File file);
	List<String> checkRepositoryClean(File repo, boolean includeIgnored);
	List<String> checkMissingCommits();
}
