turbodev.server.revisionDiff = {
	onDomReady: function() {
		var cookieName = "revisionDiff.showDiffStats";
		var $body = $(".revision-diff>.body");
		var $diffStats = $body.children(".diff-stats");
		var $diffStatsToggle = $body.find(">.title a");
		$diffStatsToggle.click(function() {
			if ($diffStats.is(":visible")) {
				$diffStats.hide();
				$diffStatsToggle.removeClass("expanded");
				Cookies.set(cookieName, "no", {expires: Infinity});
			} else {
				$diffStats.show();
				$diffStatsToggle.addClass("expanded");
				Cookies.set(cookieName, "yes", {expires: Infinity});
			}
			turbodev.server.revisionDiff.reposition();
		});
		
		var $image = $body.find("img");
		if ($image.length == 0) {
			turbodev.server.revisionDiff.reposition();
		} else {
			var loadedImages = 0;
			$image.on("load", function() {
				loadedImages++;
				if (loadedImages == $image.length) {
					turbodev.server.revisionDiff.reposition();
				}
			});
		}
		
		turbodev.server.revisionDiff.reposition();
		if (window.location.hash) {
			var $anchor = $(window.location.hash);
			if ($anchor.length != 0) {
				setTimeout(function() {
					var detailOffset = $(".revision-diff>.body>.detail").offset().top;
					if ($(window).scrollTop() <= detailOffset) {
						$(window).scrollTop(detailOffset);
					} 
					$anchor.closest(".code-comment").parent().scrollIntoView($anchor);
				}, 100);
			}
		} 
	},
	reposition: function(e) {
		if ($(".revision-diff>.body>.detail").length == 0)
			return;
		
		if (e) {
			e.stopPropagation();
			if (e.target && $(e.target).hasClass("ui-resizable"))
				return;
		}
		
		var $detail = $(".revision-diff>.body>.detail");
		var $comment = $detail.children(".comment");
		var $diffs = $detail.children(".diffs");
		var windowHeight;
		if ($comment.is(":visible")) {
			windowHeight = $(window).height();
			$detail.css("padding-left", $comment.outerWidth(true));
			var scrollLeft = $(window).scrollLeft();
			var scrollTop = $(window).scrollTop();
			$comment.css("left", $detail.offset().left - scrollLeft);
			var topOffset = $diffs.offset().top - scrollTop;
			if (topOffset <= 0) {
				$comment.css("top", 0);
			} else {
				$comment.css("top", topOffset);
			}
			var $lastDiff = $diffs.children().last();
			var commentHeight;
			if ($lastDiff.length != 0) {
				commentHeight = $lastDiff.offset().top + $lastDiff.height() - scrollTop;
			} else {
				commentHeight = $diffs.offset().top + $diffs.height() - scrollTop;
			}
			var minCommentHeight = windowHeight-52;
			if (commentHeight < minCommentHeight)
				commentHeight = minCommentHeight;
			else if (commentHeight > windowHeight)
				commentHeight = windowHeight;
			var $commentResizeHandle = $comment.children(".ui-resizable-handle");
			$commentResizeHandle.outerHeight(commentHeight - 2);
			var $commentHead = $comment.find(">.content>.head");
			$comment.find(">.content>.body").outerHeight(commentHeight-2-$commentHead.outerHeight());
		} else {
			windowHeight = 0;
			$detail.css("padding-left", "0");
		}
		var diffsHeight = $diffs.outerHeight();
		$detail.height(windowHeight>diffsHeight?windowHeight:diffsHeight);
	},
	initComment: function() {
		var $comment = $(".revision-diff>.body>.detail>.comment");
		
		if ($comment.is(":visible")) {
			var commentWidthCookieKey = "revisionDiff.comment.width";
			var commentWidth = Cookies.get(commentWidthCookieKey);
			if (!commentWidth)
				commentWidth = 400;
			$comment.outerWidth(commentWidth);
			var $commentResizeHandle = $comment.children(".ui-resizable-handle");
			var $diffs = $(".revision-diff>.body>.detail>.diffs");
			$comment.resizable({
				autoHide: false,
				handles: {"e": $commentResizeHandle},
				minWidth: 200,
				stop: function(e, ui) {
					Cookies.set(commentWidthCookieKey, ui.size.width, {expires: Infinity});
					$(window).resize();
				}
			});
		}
	}
};
$(function() {
	$(window).on("scroll resize", turbodev.server.revisionDiff.reposition);	
});
