onedev.server.blobEdit = {
	onDomReady: function(containerId) {
		var $container = $("#" + containerId);
		var $blobEdit = $container.children(".blob-edit");
		var $head = $blobEdit.children(".head");
		var $body = $blobEdit.children(".body");
		var $content = $body.children(".content");
		
	    $head.find(".edit>a").click(function() {
            if (!$(this).parent().hasClass("active")) {
                if ($head.find(".edit-plain").hasClass("active"))
                    $content.children(".tab-selection-info").val("edit-plain edit");
                else
                    $content.children(".tab-selection-info").val("save edit");
                $content.children(".submit").click();
            }
	    });
	    $head.find(".edit-plain>a").click(function() {
            if (!$(this).parent().hasClass("active")) {
                if ($head.find(".edit").hasClass("active"))
                    $content.children(".tab-selection-info").val("edit edit-plain");
                else
                    $content.children(".tab-selection-info").val("save edit-plain");
                $content.children(".submit").click();
            }
	    });
	    $head.find(".save>a").click(function() {
            if (!$(this).parent().hasClass("active")) {
                if ($head.find(".edit").hasClass("active"))
                    $content.children(".tab-selection-info").val("edit save");
                else
                    $content.children(".tab-selection-info").val("edit-plain save");
                $content.children(".submit").click();
            }
	    });
	    
	    if ($body.find(".autofit").length != 0)
	    	$body.css("overflow", "visible");
	    
	    $blobEdit.on("getViewState", function(e) {
	    	if ($content.is(":visible"))
	    		return {scroll:{left: $body.scrollLeft(), top: $body.scrollTop()}};			
	    	else
	    		return undefined;
		});
		
	    $blobEdit.on("setViewState", function(e, viewState) {
			if ($content.is(":visible") && viewState.scroll) {
				$body.scrollLeft(viewState.scroll.left);
				$body.scrollTop(viewState.scroll.top);
			}
		});
		
	    $blobEdit.on("autofit", function(e, width, height) {
			$blobEdit.outerWidth(width);
			$blobEdit.outerHeight(height);
			height = $blobEdit.height()-$head.outerHeight();
			$body.outerWidth(width).outerHeight(height);
			$body.find(".autofit:visible").first().triggerHandler("autofit", [$body.width(), $body.height()]);
		});
	    
	},
	selectTab: function($tab) {
		onedev.server.viewState.getFromViewAndSetToHistory();
		
    	var $active = $tab.parent().find(".tab.active");
    	$active.removeClass("active");
    	$tab.addClass("active");
    	var $blobEdit = $tab.closest(".blob-edit");
    	var $body = $blobEdit.children(".body");
    	
		$body.children().hide();

		if ($body.find(".autofit:visible").length != 0)
			$body.css("overflow", "visible");
		else
			$body.css("overflow", "auto");
        
        var $content = $body.children(".content");
		if ($tab.hasClass("edit")) {
			$content.show();
			$content.children(".editor").show();
            $content.children(".plain-editor").hide();
        } else if ($tab.hasClass("edit-plain")) {
			$content.show();
			$content.children(".editor").hide();
            $content.children(".plain-editor").show();
		} else {
			$body.children(".commit-options").show();
        }
        $(window).resize();
	},
	onNameChanging: function(containerId, addingFile, recreateCallback) {
		var $body = $("#" + containerId + ">.blob-edit>.body");
		var contentModified = $body.find("form.dirty").length != 0;
		if (addingFile && !contentModified)
			recreateCallback();
		else
			$body.find(".name-changing-listener").trigger("nameChanging");
	}
};
