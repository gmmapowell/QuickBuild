package test.quickbuild.scaffolding;

import java.io.File;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.git.GitRecord;

public class GitHandler implements Action {

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("GitHandler");
	}

	@Override
	public Object invoke(Invocation arg0) throws Throwable {
		File file = (File) arg0.getParameter(2);
		GitRecord ret = new GitRecord(file);
		File newFile = new File(file.getParentFile(), file.getName() + ".new");
		ret.generates(newFile);
		FileUtils.writeFile(newFile, "hello");
		ret.markDirty();
		return ret;
	}

}
