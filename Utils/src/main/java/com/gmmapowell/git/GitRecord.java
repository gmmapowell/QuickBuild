package com.gmmapowell.git;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;

public class GitRecord {
	private final File source;
	private File generates;
	private boolean dirty;
	private boolean error;

	GitRecord(File file) {
		this.source = file;
	}

	public void setError() {
		error = true;
	}

	/** Call commit no matter what.
	 * If there are any errors, both files will be deleted.
	 * Otherwise, if there is only one file, that is saved;
	 * if both were needed, the old one is unlinked and the new one copied across  
	 */
	public void commit()
	{
		// handle the error case first
		if (error)
		{
			if (source.exists())
				source.delete();
			if (generates.exists())
				generates.delete();
		}
		else if (source.exists() && generates.exists()) // the typical something might have changed case
		{
			if (dirty)
			{ // they are different, move ...
				boolean fd = source.delete();
				if (!fd)
					throw new UtilException("Could not delete the file " + source + " when renaming " + generates);
				boolean renameWorked = generates.renameTo(source);
				if (!renameWorked)
					throw new UtilException("Could not rename " + generates + " to " + source);
			}
			else
			{
				// they're the same, the new one is uninteresting
				generates.delete();
			}
		}
		else if (!source.exists() && generates.exists()) // we didn't have a comparison file
		{
			boolean renameWorked = generates.renameTo(source);
			if (!renameWorked)
				throw new UtilException("Could not rename " + generates + " to " + source);
		}
		else
			throw new UtilException("I don't think this case should be able to happen");
	}

	boolean sourceExists() {
		return source != null;
	}

	void setDirty() {
		dirty = true;
	}

	void generates(File newFile) {
		generates = newFile;
	}

	public boolean isDirty() {
		return dirty;
	}

}
