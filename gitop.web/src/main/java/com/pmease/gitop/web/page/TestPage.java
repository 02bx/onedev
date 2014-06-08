package com.pmease.gitop.web.page;

import java.io.Serializable;
import java.util.List;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;

import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.validation.ClassValidating;
import com.pmease.commons.validation.Validatable;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.BeanEditor;

@SuppressWarnings("serial")
public class TestPage extends WebPage {

	private Bean bean = new Bean();
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Form<?> form = new Form<Void>("form");
		add(form);

		BeanEditor<Serializable> editor = BeanContext.edit("editor", bean); 
		form.add(editor);
		form.add(new FencedFeedbackPanel("feedback", editor));
		
		add(BeanContext.view("viewer", bean));
	}
	
	@Editable
	public static class Bean implements Serializable {
		private String name;
		
		private List<Pet> pets;

		@Editable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Valid
		@Editable
		public List<Pet> getPets() {
			return pets;
		}

		public void setPets(List<Pet> pets) {
			this.pets = pets;
		}

	}
	
	@Editable
	public static interface Pet extends Serializable {
	}

	@Editable
	@ClassValidating
	public static class Cat implements Pet, Validatable {
		private String name;
		
		private String mouse;

		private Dog pet;
		
		@Editable
		@NotEmpty
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Editable
		@NotEmpty
		public String getMouse() {
			return mouse;
		}

		public void setMouse(String mouse) {
			this.mouse = mouse;
		}

		@Editable
		@Valid
		public Dog getPet() {
			return pet;
		}

		public void setPet(Dog pet) {
			this.pet = pet;
		}

		@Override
		public boolean isValid(ConstraintValidatorContext context) {
			if (name != null && mouse != null) {
				if (name.equals("tom") && mouse.equals("jerry")) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate("tom can not eat jerry").addConstraintViolation();
					context.buildConstraintViolationWithTemplate("tom be patient").addPropertyNode("name").addConstraintViolation();
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		
	}
	
	@Editable
	public static class Dog implements Pet {
		private String name;
		
		private int age;

		@Editable
		@NotEmpty
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Editable
		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
		
	}
}
