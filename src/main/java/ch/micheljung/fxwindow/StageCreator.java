package ch.micheljung.fxwindow;

import javafx.stage.Modality;
import javafx.stage.Window;

public interface StageCreator extends StageConfigurer {

  StageCreator initOwner(Window owner);

  StageCreator initModality(Modality modality);
}
