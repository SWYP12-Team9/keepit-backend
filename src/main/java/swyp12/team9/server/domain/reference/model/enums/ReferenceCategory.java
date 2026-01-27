package swyp12.team9.server.domain.reference.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceCategory {
  A("A"),
  B("B"),
  C("C"),
  D("D"),
  E("E");

  private final String title;
}
