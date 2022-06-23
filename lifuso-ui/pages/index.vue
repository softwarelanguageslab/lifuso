<script lang="ts" setup>
import {
  Tab,
  TabGroup,
  TabList,
  TabPanels,
  TabPanel,
  Combobox,
  ComboboxInput,
  ComboboxOptions,
  ComboboxOption,
  Switch,
} from '@headlessui/vue';
import { Icon } from '@iconify/vue';
import type { ParsedContent } from '@nuxt/content/dist/runtime/types';

useHead({ title: 'Home' });

const searchText = ref('');
const limit = ref(9);
const skip = ref(0);
const paginate = ref(false);
const numberFeatures = ref(0);

const compare1 = ref('');
const compare2 = ref('');
const queryFirstSearch = ref('');
const querySecondSearch = ref('');

let sharedFeaturesLib1 = ref([]);
let sharedFeaturesLib2 = ref([]);

// Search the comparison for the libraries selected
const { data: comparisonShared, refresh: refreshShared } = await useAsyncData('comparisonShared', () =>
  queryContent<ParsedContent>(`/comparison/${compare1.value}_${compare2.value}/shared`)
  .find()
);

const { data: comparisonUniqueA, refresh: refreshUniqueA } = await useAsyncData('comparisonUniqueA', () =>
  queryContent<ParsedContent>(`/comparison/${compare1.value}_${compare2.value}/unique/${compare1.value}`)
  .find()
);

const { data: comparisonUniqueB, refresh: refreshUniqueB } = await useAsyncData('comparisonUniqueB', () =>
  queryContent<ParsedContent>(`/comparison/${compare1.value}_${compare2.value}/unique/${compare2.value}`)
  .find()
);

// Search all features
const { data: allFeaturesJSON } = await useAsyncData('search', () =>
    queryContent<ParsedContent>('/features')
    .find()
);

// Fisher–Yates Shuffle: https://bost.ocks.org/mike/shuffle/
function shuffle(array) {
  let currentIndex = array.length,  randomIndex;

  // While there remain elements to shuffle.
  while (currentIndex != 0) {

    // Pick a remaining element.
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;

    // And swap it with the current element.
    [array[currentIndex], array[randomIndex]] = [
      array[randomIndex], array[currentIndex]];
  }

  return array;
}

const allFeaturesConst = shuffle(allFeaturesJSON.value[0].body);
let allFeatures = allFeaturesConst;

let features = ref(allFeatures.slice(0, limit.value));
numberFeatures.value = allFeatures.length;

function searchFeatures(text) {
  return allFeaturesConst.filter((feature) => {
    return feature.name.includes(text);
  })
}

// All libraries that will the comboboxes display
const allLibraries: string[] = [...new Set(allFeaturesConst.map(({library}) => library))];

const librariesFirst = computed(() =>
  queryFirstSearch.value === ''
    ? []
    : (() => {
      const firstSearch = allLibraries.filter((libraryName) => 
        libraryName.includes(queryFirstSearch.value));

      if (compare2.value !== '') {
        return firstSearch.filter((libraryName) => libraryName !== compare2.value);
      } else {
        return firstSearch;
      }
    })()
  );

const librariesSecond = computed(() =>
  querySecondSearch.value === ''
    ? []
    : (() => {
      const secondSearch = allLibraries.filter((libraryName) => 
        libraryName.includes(querySecondSearch.value));

      if (compare1.value !== '') {
        return secondSearch.filter((libraryName) => libraryName !== compare1.value);
      } else {
        return secondSearch;
      }
    })()
  );

// Pagination in the features
const prev = () => {
  skip.value -= limit.value;
  paginate.value = true;
};
const next = () => {
  skip.value += limit.value;
  paginate.value = true;
};

function processSharedNames(features) {
  let allPairNames = [];

  features.forEach(feature => {
    let nameDivided = feature.name.split(",");
    allPairNames = [...new Set([...allPairNames,...nameDivided])];
  });

  return allPairNames;
}

let checkedNames = [];

function switchActivated(namePair) {
  let element = document.getElementById(namePair);

  if (checkedNames.length == 0) {
    checkedNames.push(element);
  } else {
    let previousElement = checkedNames.pop();

    if (previousElement.id !== element.id) {
      checkedNames.push(element);
      previousElement.checked = !previousElement.checked;
    }
  }

  // Search for the features that are shared between the two libraries with the selected name
  if (checkedNames.length == 1) {
    let name = checkedNames[0].id;
    let sharedFeatures = comparisonShared.value[0].body.filter(feature => feature.name.includes(name));
    sharedFeaturesLib1.value = sharedFeatures.filter(feature => feature.library === compare1.value);

    sharedFeaturesLib2.value = sharedFeatures.filter(feature => feature.library === compare2.value);
  } else {
    sharedFeaturesLib1.value = [];
    sharedFeaturesLib2.value = [];
  }
}

// Monitor the specified values
watch([searchText, skip, compare1, compare2], () => {
  refreshShared();
  refreshUniqueA();
  refreshUniqueB();

  allFeatures = searchFeatures(searchText.value);

  comparisonShared.value = [];
  sharedFeaturesLib1.value = [];
  sharedFeaturesLib2.value = [];

  if (paginate.value) {
    paginate.value = false;

  } else {
    skip.value = 0;
  }
  features.value = allFeatures.slice(skip.value, skip.value + limit.value);
  numberFeatures.value = allFeatures.length;
});
</script>

<template>
  <div class="page">
    <Title />

    <TabGroup>
      <TabList class="tab-list">
        <Tab v-slot="{ selected }" as="template">
            <button :class="[selected ? 'selected-tab' : 'not-selected-tab']">
              Search
            </button>
          </Tab>
        <Tab v-slot="{ selected }" as="template">
            <button :class="[selected ? 'selected-tab' : 'not-selected-tab']">
              Compare
            </button>
          </Tab>
      </TabList>

      <TabPanels class="tab-panels">
        <TabPanel class="tab-panel">
          <SearchInput
            v-model="searchText"
            placeholder="Search Feature"
          />

          <ul class="feature-list">
            <li
              v-for="feature in features"
              :key="feature.title"
              class="feature-item"
            >
              <article class="feature-item__card">
                <div class="title-card" v-html="feature.library"></div>
                <div v-html="feature.name_data" style="margin-bottom: 1rem;"></div>
                <div v-html="feature.heading" style="margin-bottom: 1rem;"></div>
                <div v-html="feature.code"></div>
              </article>
            </li>
          </ul>

          <Pagination
            :skip="skip"
            :limit="limit"
            :numberFeatures="numberFeatures"
            @click:prev="prev"
            @click:next="next"
          />
        </TabPanel>

        <TabPanel class="tab-panel">
          <div class="compare-container">
            <Combobox v-model="compare1">
              <div class="combobox">
                <ComboboxInput @change="queryFirstSearch = $event.target.value" />
                <ComboboxOptions class="combobox__options">
                  <ComboboxOption
                    as="template"
                    v-slot="{ active, selected }"
                    v-for="library in librariesFirst"
                    :key="library"
                    :value="library"
                  >
                    <li :class="{'selected-tab': active, 'not-selected-tab': !active}">
                        <div class="elements-li">
                          <Icon icon="heroicons-solid:check-circle" v-show="selected"/>
                          {{ library }}
                        </div>
                    </li>
                  </ComboboxOption>
                </ComboboxOptions>
              </div>
            </Combobox>

            <Combobox v-model="compare2">
              <div class="combobox">
                <ComboboxInput @change="querySecondSearch = $event.target.value" />
                <ComboboxOptions class="combobox__options">
                  <ComboboxOption
                    as="template"
                    v-slot="{ active, selected }"
                    v-for="library in librariesSecond"
                    :key="library"
                    :value="library"
                  >
                    <li :class="{'selected-tab': active, 'not-selected-tab': !active}">
                      <div class="elements-li">
                        <Icon icon="heroicons-solid:check-circle" v-show="selected" /> 
                        {{ library }}
                      </div>
                    </li>
                  </ComboboxOption>
                </ComboboxOptions>
              </div>
            </Combobox>
          </div>

          <!-- List of shared features between the libraries -->
          <div class="comparison-list" v-if="comparisonShared.length">
            <div class="title-comparison">
              <p>Shared Features</p>
            </div>
            
            <ul class="switch-list">
              <li
                v-for="name in processSharedNames(comparisonShared[0].body)"
                :key="name"
                class="switch-item"
              >
                <div class="switch-item__container">
                  <label class="switch">
                    <input type="checkbox" v-on:click="switchActivated(name)" :id="name">
                    <span class="slider round"></span>
                  </label>
                  <div v-html="name"></div>
                </div>
              </li>
            </ul>

            <div style="display: flex; justify-content: center; margin-top: 2%;">
              <div style="display: inline-block; width: 48%; border: 1px solid; border-radius: 30px;" v-if="sharedFeaturesLib1.length">
                <div class="title-comparison">
                    <p>{{compare1}} shared features</p>
                </div>
                <ul class="comparison-list">
                  <li
                    v-for="feature in sharedFeaturesLib1"
                    :key="feature.title"
                    class="feature-item"
                  >
                    <article class="feature-item__card">
                      <div class="title-card" v-html="feature.library"></div>
                      <div v-html="feature.name_data" style="margin-bottom: 1rem;"></div>
                      <div v-html="feature.heading" style="margin-bottom: 1rem;"></div>
                      <div v-html="feature.code"></div>
                    </article>
                  </li>
                </ul>
              </div>

              <div style="display: inline-block; width: 48%; border: 1px solid; margin: var(--size-2); border-radius: 30px;"  v-if="sharedFeaturesLib2.length">
                <div class="title-comparison">
                  <p>{{compare2}} shared features</p>
                </div>
                <ul class="comparison-list">
                  <li
                    v-for="feature in sharedFeaturesLib2"
                    :key="feature.title"
                    class="feature-item"
                  >
                    <article class="feature-item__card">
                      <div class="title-card" v-html="feature.library"></div>
                      <div v-html="feature.name_data" style="margin-bottom: 1rem;"></div>
                      <div v-html="feature.heading" style="margin-bottom: 1rem;"></div>
                      <div v-html="feature.code"></div>
                    </article>
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- List of unique features for library A -->
          <div class="comparison-list" v-if="comparisonUniqueA.length">
            <div class="title-comparison">
              <p>Unique features for library {{compare1}}</p>
            </div>
            <ul class="comparison-list">
              <li
                v-for="feature in comparisonUniqueA[0].body"
                :key="feature.title"
                class="feature-item"
              >
                <article class="feature-item__card">
                  <div class="title-card" v-html="feature.library"></div>
                  <div v-html="feature.name_data" style="margin-bottom: 1rem;"></div>
                  <div v-html="feature.heading" style="margin-bottom: 1rem;"></div>
                  <div v-html="feature.code"></div>
                </article>
              </li>
            </ul>
          </div>

          <!-- List of unique features for library B -->
          <div class="comparison-list" v-if="comparisonUniqueB.length">
            <div class="title-comparison">
              <p>Unique features for library {{compare2}}</p>
            </div>
            <ul class="comparison-list">
              <li
                v-for="feature in comparisonUniqueB[0].body"
                :key="feature.title"
                class="feature-item"
              >
                <article class="feature-item__card">
                  <div class="title-card" v-html="feature.library"></div>
                  <div v-html="feature.name_data" style="margin-bottom: 1rem;"></div>
                  <div v-html="feature.heading" style="margin-bottom: 1rem;"></div>
                  <div v-html="feature.code"></div>
                </article>
              </li>
            </ul>
          </div>
        </TabPanel>
      </TabPanels>
    </TabGroup>
  </div>
</template>

<style lang="scss" scoped>
.page {
  display: grid;
  gap: var(--size-10);
  place-items: center;
}

.tab-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--size-3);
  place-items: center;

  button {
    --_bg: unset;
    --_border: unset;
    --_ink-shadow: unset;
    box-shadow: unset;
  }
}

.selected-tab {
  background-color: var(--surface-4);
}

.not-selected-tab {
  background-color: var(--surface-1);
}

.elements-li {
  width: 100%;
  display: inline-flex; 
  align-items: left;
  vertical-align: middle;
  gap: var(--size-2);
}

.tab-panels {
  width: 100%;
}
.tab-panel {
  display: grid;
  gap: var(--size-8);
  place-items: center;
}

.feature-list {
  width: 100%;
  list-style: none;
  margin-left: 0;
  padding-left: 0;
  display: grid;
  gap: var(--size-3);
  grid-template-columns: repeat(
    auto-fit,
    minmax(min(var(--size-14), 100%), 1fr)
  );
  place-items: flex-start;

  .feature-item {
    width: 100%;
    height: 100%;
  }
}

.title-comparison {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: center;
  margin-bottom: 20px;

  p {
    font-size: x-large;
    margin-left: 5%;
    font-weight: 800;
    display: inline-block;
  }
}

.comparison-list {
  overflow: auto;
  white-space: nowrap;
  width: 100%;
  vertical-align: top;
  border-radius: 40px;

  .feature-item {
    display: inline-block;
    vertical-align:top;
    margin-right:20px;
    white-space:normal;
    padding-top: 1%;
    padding-left: 0;
    padding-bottom: 1%;
    border: none;
  }
}

.feature-item__card {
  background-color: var(--surface-2);
  border-radius: var(--radius-3);
  box-shadow: var(--shadow-1);
  padding: var(--size-3);
  width: 100%;
  height: 100%;

  &:hover {
    box-shadow: var(--shadow-5);
  }

  :deep(pre code) {
    display: block;
    overflow-x: scroll;
    background-color: var(--surface-4);
    border-radius: var(--radius-3);
    padding: var(--size-3);
  }

  & > :deep(div:first-of-type) {
    display: grid;
    gap: var(--size-3);
  }
}

.title-card {
  font-size: medium;
  text-align: center;
  font-weight: 600;
  color: rgb(204, 0, 0);
  border-bottom: 2px solid black;
}

.compare-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--size-10);
  place-items: center;
  width: 75%;
  margin-inline: auto;
}

.combobox {
  position: relative;
  width: 100%;

  input {
    width: 100%;
  }

  ul {
    list-style: none;

    &,
    li {
      padding: 0;
      margin: 0;
    }
  }
}

.combobox__options {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  z-index: 1;
  box-shadow: var(--shadow-3);
  padding: var(--size-3) !important;
  max-height: 50rem;
}

.switch-list {
  width: 100%;
  list-style: none;
  margin-left: 0;
  padding-left: 0;
  display: grid;
  gap: var(--size-3);
  grid-template-columns: repeat(
    auto-fit,
    minmax(min(var(--size-12), 100%), 1fr)
  );
  place-items: center;

  .switch-item {
    width: 100%;
    height: 100%;
    display: inline-flex;
  }
}

.switch-item__container {
  display: inline-flex;
}

.switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 25px;
}

/* Hide default HTML checkbox */
.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

/* The slider */
.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  -webkit-transition: .4s;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 17px;
  width: 17px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  -webkit-transition: .4s;
  transition: .4s;
}

input:checked + .slider {
  background-color: #2196F3;
}

input:focus + .slider {
  box-shadow: 0 0 1px #2196F3;
}

input:checked + .slider:before {
  -webkit-transform: translateX(12px);
  -ms-transform: translateX(12px);
  transform: translateX(12px);
}

/* Rounded sliders */
.slider.round {
  border-radius: 34px;
}

.slider.round:before {
  border-radius: 50%;
}

</style>
