package io.onedev.server.search.hit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.jsymbol.util.HighlightableLabel;
import io.onedev.jsymbol.util.NoAntiCacheImage;
import io.onedev.utils.Range;

public class FileHit extends QueryHit {

	private static final long serialVersionUID = 1L;

	private final Range matchRange;
	
	public FileHit(String blobPath, @Nullable Range matchRange) {
		super(blobPath, null);
		this.matchRange = matchRange;
	}

	@Override
	public String toString() {
		return getBlobPath();
	}

	@Override
	public Component render(String componentId) {
		String fileName = getBlobPath();
		if (fileName.contains("/")) 
			fileName = StringUtils.substringAfterLast(fileName, "/");
		
		return new HighlightableLabel(componentId, fileName, matchRange);
	}

	@Override
	public Image renderIcon(String componentId) {
		return new NoAntiCacheImage(componentId, new PackageResourceReference(FileHit.class, "file.png"));
	}

	@Override
	public String getNamespace() {
		if (getBlobPath().contains("/")) 
			return StringUtils.substringBeforeLast(getBlobPath(), "/");
		else 
			return null;
	}

	public Range getMatchRange() {
		return matchRange;
	}

}
