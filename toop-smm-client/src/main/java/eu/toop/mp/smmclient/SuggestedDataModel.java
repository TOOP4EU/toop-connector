package eu.toop.mp.smmclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public class SuggestedDataModel {
  private final Map<String, List<String>> m_aMap = new HashMap<> ();

  public void addConceptToBeMapped (@Nonnull @Nonempty final String sConceptScheme,
                                    @Nonnull @Nonempty final String sConceptValue) {
    ValueEnforcer.notEmpty (sConceptScheme, "Scheme");
    ValueEnforcer.notEmpty (sConceptValue, "Value");
    m_aMap.computeIfAbsent (sConceptScheme, k -> new ArrayList<> ()).add (sConceptValue);
  }
}
