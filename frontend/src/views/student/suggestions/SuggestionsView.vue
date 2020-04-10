<template>
  <v-card class="table">
    <v-data-table
      :headers="headers"
      :custom-filter="customFilter"
      :items="suggestions"
      :search="search"
      multi-sort
      :mobile-breakpoint="0"
      :items-per-page="15"
      :footer-props="{ itemsPerPageOptions: [15, 30, 50, 100] }"
    >
      <template v-slot:top>
        <v-card-title>
          <v-text-field
            v-model="search"
            append-icon="search"
            label="Search"
            class="mx-2"
          />

          <v-spacer />
          <v-btn
            color="primary"
            dark
            @click="newSuggestion"
            data-cy="createSuggestionButton"
            >New Suggestion</v-btn
          >
        </v-card-title>
      </template>

      <template v-slot:item.title="{ item }">
        <p
          v-html="convertMarkDownNoFigure(item.question.title, null)"
          @click="showSuggestionDialog(item)"
      /></template>

      <template v-slot:item.content="{ item }">
        <p
          v-html="convertMarkDownNoFigure(item.question.content, null)"
          @click="showSuggestionDialog(item)"
      /></template>

      <template v-slot:item.image="{ item }">
        <v-file-input
          show-size
          dense
          small-chips
          @change="handleFileUpload($event, item)"
          accept="image/*"
        />
      </template>

      <template v-slot:item.approved="{ item }">
        <v-chip :color="item.approved ? 'green' : 'red'" small>
          <span>{{ item.approved ? 'Yes' : 'No' }}</span>
        </v-chip>
      </template>

      <template v-slot:item.action="{ item }">
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-icon
              small
              class="mr-2"
              v-on="on"
              @click="showSuggestionDialog(item)"
              >visibility</v-icon
            >
          </template>
          <span>Show Suggestion</span>
        </v-tooltip>
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-icon small class="mr-2" v-on="on" @click="editSuggestion(item)"
              >edit</v-icon
            >
          </template>
          <span>Edit Suggestion</span>
        </v-tooltip>
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-icon
              small
              class="mr-2"
              v-on="on"
              @click="deleteSuggestion(item)"
              color="red"
              data-cy="deleteSuggestionButton"
              >delete</v-icon
            >
          </template>
          <span>Delete Suggestion</span>
        </v-tooltip>
      </template>
    </v-data-table>
    <edit-suggestion-dialog
      v-if="currentSuggestion"
      v-model="editSuggestionDialog"
      :suggestion="currentSuggestion"
      v-on:save-suggestion="onSaveSuggestion"
    />
    <show-suggestion-dialog
      v-if="currentSuggestion"
      :dialog="suggestionDialog"
      :suggestion="currentSuggestion"
      v-on:close-show-suggestion-dialog="onCloseShowSuggestionDialog"
    />
  </v-card>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { convertMarkDownNoFigure } from '@/services/ConvertMarkdownService';
import Suggestion from '@/models/management/Suggestion';
import Image from '@/models/management/Image';
import ShowSuggestionDialog from '@/views/student/suggestions/ShowSuggestionDialog.vue';
import EditSuggestionDialog from '@/views/student/suggestions/EditSuggestionDialog.vue';

@Component({
  components: {
    'show-suggestion-dialog': ShowSuggestionDialog,
    'edit-suggestion-dialog': EditSuggestionDialog
  }
})
export default class SuggestionsView extends Vue {
  suggestions: Suggestion[] = [];
  currentSuggestion: Suggestion | null = null;
  editSuggestionDialog: boolean = false;
  suggestionDialog: boolean = false;
  search: string = '';

  headers: object = [
    { text: 'Title', value: 'title', align: 'center' },
    { text: 'Question', value: 'content', align: 'left' },
    { text: 'Approved', value: 'approved', align: 'center' },
    {
      text: 'Creation Date',
      value: 'creationDate',
      align: 'center'
    },
    {
      text: 'Image',
      value: 'image',
      align: 'center',
      sortable: false
    },
    {
      text: 'Actions',
      value: 'action',
      align: 'center',
      sortable: false
    }
  ];

  @Watch('editSuggestionDialog')
  closeError() {
    if (!this.editSuggestionDialog) {
      this.currentSuggestion = null;
    }
  }

  async created() {
    await this.$store.dispatch('loading');
    try {
      this.suggestions = await RemoteServices.getStudentSuggestions();
    } catch (error) {
      await this.$store.dispatch('error', error);
    }
    await this.$store.dispatch('clearLoading');
  }

  customFilter(value: string, search: string, suggestion: Suggestion) {
    // noinspection SuspiciousTypeOfGuard,SuspiciousTypeOfGuard
    return (
      search != null &&
      JSON.stringify(suggestion)
        .toLowerCase()
        .indexOf(search.toLowerCase()) !== -1
    );
  }

  convertMarkDownNoFigure(text: string, image: Image | null = null): string {
    return convertMarkDownNoFigure(text, image);
  }

  async handleFileUpload(event: File, suggestion: Suggestion) {
    if (suggestion.id) {
      try {
        const imageURL = await RemoteServices.uploadSuggestionImage(
          event,
          suggestion.id
        );
        suggestion.question.image = new Image();
        suggestion.question.image.url = imageURL;
        confirm('Image ' + imageURL + ' was uploaded!');
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }

  showSuggestionDialog(suggestion: Suggestion) {
    this.currentSuggestion = suggestion;
    this.suggestionDialog = true;
  }

  onCloseShowSuggestionDialog() {
    this.suggestionDialog = false;
  }

  newSuggestion() {
    this.currentSuggestion = new Suggestion();
    this.editSuggestionDialog = true;
  }

  editSuggestion(suggestion: Suggestion) {
    this.currentSuggestion = suggestion;
    this.editSuggestionDialog = true;
  }

  async onSaveSuggestion(suggestion: Suggestion) {
    this.suggestions = this.suggestions.filter(q => q.id !== suggestion.id);
    this.suggestions.unshift(suggestion);
    this.editSuggestionDialog = false;
    this.currentSuggestion = null;
  }

  async deleteSuggestion(toDelete: Suggestion) {
    if (
      toDelete.id &&
      !toDelete.approved &&
      confirm('Are you sure you want to delete this suggestion?')
    ) {
      try {
        await RemoteServices.deleteSuggestion(toDelete.id);
        this.suggestions = this.suggestions.filter(
          suggestion => suggestion.id != toDelete.id
        );
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.question-textarea {
  text-align: left;

  .CodeMirror,
  .CodeMirror-scroll {
    min-height: 200px !important;
  }
}
.option-textarea {
  text-align: left;

  .CodeMirror,
  .CodeMirror-scroll {
    min-height: 100px !important;
  }
}
</style>