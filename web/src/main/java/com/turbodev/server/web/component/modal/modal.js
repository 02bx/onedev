turbodev.server.modal = {
	onDomReady: function(containerId, closeCallback) {
		var $modal = $("#" + containerId);
		$modal.data("closeCallback", closeCallback);
		
		$modal.data("keydown", function(e) {
			if (e.keyCode == 27 
					&& $(".select2-drop:visible").length == 0 
					&& $("body>.floating").length == 0 
					&& $(".atwho-view:visible").length == 0
					&& $modal.nextAll(".modal").length == 0) {
				turbodev.server.modal.close($modal, true);
			}
		});
		
		// Use keydown as keypress does not work in chrome/safari
		$(document).on("keydown", $modal.data("keydown"));
		
		$modal.modal({backdrop: "static", "keyboard": false});
	}, 
	
	close: function($modal, callCloseCallback) {
		if (callCloseCallback)
			$modal.data("closeCallback")();
		
		$(document).off("keydown", $modal.data("keydown"));
		
		$modal.modal("hide").remove();
	}
	
};