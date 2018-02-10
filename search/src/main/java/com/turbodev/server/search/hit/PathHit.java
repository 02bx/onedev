package com.turbodev.server.search.hit;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.turbodev.utils.Range;
import com.turbodev.jsymbol.util.HighlightableLabel;
import com.turbodev.jsymbol.util.NoAntiCacheImage;

public class PathHit extends QueryHit {

	private static final long serialVersionUID = 1L;

	private final Range matchRange;
	
	public PathHit(String blobPath, @Nullable Range matchRange) {
		super(blobPath, null);
		this.matchRange = matchRange;
	}

	@Override
	public String toString() {
		return getBlobPath();
	}

	@Override
	public Component render(String componentId) {
		return new HighlightableLabel(componentId, getBlobPath(), matchRange);
	}

	@Override
	public Image renderIcon(String componentId) {
		return new NoAntiCacheImage(componentId, new PackageResourceReference(PathHit.class, "file.png"));
	}

	@Override
	public String getNamespace() {
		return null;
	}

	public Range getMatchRange() {
		return matchRange;
	}

}
