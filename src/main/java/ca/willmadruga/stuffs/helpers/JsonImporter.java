
package ca.willmadruga.stuffs.helpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ca.willmadruga.stuffs.domain.CardModel;
import ca.willmadruga.stuffs.domain.SetModel;
import ca.willmadruga.stuffs.persistence.CardEntity;
import ca.willmadruga.stuffs.persistence.MechanicsEntity;
import ca.willmadruga.stuffs.persistence.Repos.CardsRepo;
import ca.willmadruga.stuffs.persistence.Repos.MechanicsRepo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by wmad on 2015-11-21.
 */
@Component
public class JsonImporter {

    private static final Logger log = LoggerFactory.getLogger(JsonImporter.class);

    @Value("${importer.json.directory}")
    private String directory;

    @Value("${importer.json.filename}")
    private String filename;

    @Autowired
    private CardsRepo cardsRepo;

    @Autowired
    private MechanicsRepo mechanicsRepo;

    private List<CardEntity> createCards(final SetModel setModel) {

        final List<CardEntity> cards = new ArrayList<>();
        for (final CardModel cardModel : setModel.getList()) {

            CardEntity card = cardsRepo.findByCardIdentifier(cardModel.getId());

            if (card == null) {
                card = new CardEntity();
            }

            card.setCardIdentifier(cardModel.getId());
            card.setName(cardModel.getName());
            card.setSetName(setModel.getSetName());
            card.setType(cardModel.getType());
            card.setFaction(cardModel.getFaction());
            card.setRarity(cardModel.getRarity());
            card.setCost(cardModel.getCost());
            card.setAttack(cardModel.getAttack());
            card.setHealth(cardModel.getHealth());
            card.setDurability(cardModel.getDurability());

            if (!StringUtils.isEmpty(cardModel.getText())) {
                card.setText(cardModel.getText().replace("$", "").replace("#", ""));
            } else {
                card.setText(cardModel.getText());
            }

            card.setElite(cardModel.getElite());
            card.setInPlayText(cardModel.getInPlayText());
            card.setFlavor(cardModel.getFlavor());
            card.setArtist(cardModel.getArtist());
            card.setCollectible(cardModel.getCollectible());
            card.setRace(cardModel.getRace());
            card.setPlayerClass(cardModel.getPlayerClass());
            card.setHowToGet(cardModel.getHowToGet());
            card.setHowToGetGold(cardModel.getHowToGetGold());

            // Process card mechanics
            createCardMechanics(cardModel, card);

            cards.add(card);
        }

        return cards;
    }

    /**
     * Import data from Json.
     */
    public void importData() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final File jsonFile = new File(directory + filename);

        final List<SetModel> setModelList = processJson(mapper, jsonFile);

        for (final SetModel setModel : setModelList) {

            final List<CardEntity> setCards = createCards(setModel);
            cardsRepo.save(setCards); //there is no merge... (look for a proper interface to extend from.)
            log.info("Saved cards for {}", setModel.getSetName());

        }
    }

    private void createCardMechanics(CardModel cardModel, CardEntity card) {
        if (cardModel.getMechanics() != null) {
            card.setMechanics(new HashSet<MechanicsEntity>());
            for (final String mechanics : cardModel.getMechanics()) {

                // check for existence and create new if not found.
                MechanicsEntity mechanicsEntity = mechanicsRepo.findByMechanic(mechanics);
                if (mechanicsEntity == null) {
                    mechanicsEntity = mechanicsRepo.save(new MechanicsEntity(mechanics));
                }

                card.getMechanics().add(mechanicsEntity);
            }
        }
    }

    private List<SetModel> processJson(final ObjectMapper mapper, final File jsonFile) throws IOException {

        final List<SetModel> setModelList = new ArrayList<>();

        final JsonNode node = mapper.readTree(jsonFile);

        final Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {

            final String fieldName = fieldNames.next();
            final List<JsonNode> cards = node.findValues(fieldName);
            final SetModel setModel = new SetModel();

            setModel.setSetName(fieldName);
            setModel.setList(new ArrayList<CardModel>());

            for (final JsonNode content : cards) {
                final Iterator cardIterator = content.elements();
                while (cardIterator.hasNext()) {

                    final ObjectNode card = (ObjectNode) cardIterator.next();

                    final String cardJson = card.toString();
                    final CardModel cardModel = mapper.readValue(cardJson, CardModel.class);
                    setModel.getList().add(cardModel);
                }

                setModelList.add(setModel);
            }

        }

        return setModelList;

    }

}
