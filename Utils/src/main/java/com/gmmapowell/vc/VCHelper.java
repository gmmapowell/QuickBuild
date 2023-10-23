package com.gmmapowell.vc;

import java.io.File;

import com.gmmapowell.git.GitRecord;
import com.gmmapowell.utils.OrderedFileList;

public interface VCHelper {
	GitRecord checkFiles(boolean doComparison, OrderedFileList files, File file);
}
