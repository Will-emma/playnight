package com.takima.backskeleton.DTO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.takima.backskeleton.models.TypeJeu;

import java.io.IOException;

// Contrôles limités à JeuDto : ne changent pas la désérialisation des requêtes RAWG.
public final class JeuDtoDeserializers {
    private JeuDtoDeserializers() { }

    public static final class Entier extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) return parser.getIntValue();
            // Conserve la compatibilité avec les entiers transmis sous forme de chaînes.
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                try {
                    return Integer.valueOf(parser.getText().trim());
                } catch (NumberFormatException exception) {
                    throw context.weirdStringException(parser.getText(), Integer.class, "Le nombre de joueurs doit être un entier.");
                }
            }
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
    }

    public static final class Type extends JsonDeserializer<TypeJeu> {
        @Override
        public TypeJeu deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                try {
                    return TypeJeu.valueOf(parser.getText());
                } catch (IllegalArgumentException exception) {
                    throw context.weirdStringException(parser.getText(), TypeJeu.class, "Le type doit être SOCIETE ou JEU_VIDEO.");
                }
            }
            // Un nombre ne doit pas être interprété comme la position d'une valeur de l'enum.
            return (TypeJeu) context.handleUnexpectedToken(TypeJeu.class, parser);
        }
    }
}
