/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component } from "react";
import axios from "axios";

class SearchResult extends Component {
  render() {
    const item = this.props.item;
    const receiverPrefix =
      item.receiverName == null ? "" : item.receiverName + ".";
    return (
      <div>
        <p>
          {receiverPrefix}
          {item.name} ({item.score}) @ {item.groupId}:{item.artifactId}:
          {item.version}{" "}
        </p>
      </div>
    );
  }
}

function ResultList(props) {
  const listItems = props.searchResults.map(item => (
    <li key={item.id}>
      <SearchResult item={item} />
    </li>
  ));
  return <ul>{listItems}</ul>;
}

class ArtifactSearch extends Component {
  state = {
    query: "",
    includeClasses: "",
    includeExtensionMethods: "",
    includeGlobalMethods: "",
    searchResults: []
  };
  handleInputChange = async event => {
    //event.preventDefault();

    // Promise is resolved and value is inside of the response const.
    // https://jsonplaceholder.typicode.com/users
    // http://0.0.0.0:8080/searchArtifact
    // /searchArtifact
    console.log(
      "include classes:",
      this.includeClassesInput.checked,
      "include extension",
      this.includeExtensionMethodsInput.checked,
      "include global",
      this.includeGlobalMethodsInput.checked
    );
    const response = await axios.get(`/searchArtifact`, {
      params: {
        query: this.search.value,
        includeClasses: this.includeClassesInput.checked,
        includeExtensionMethods: this.includeExtensionMethodsInput.checked,
        includeGlobalMethods: this.includeGlobalMethodsInput.checked
      },
      headers: { "Content-Type": "application/json" }
    });
    console.log(response);
    console.log(response.data);
    this.setState({
      query: this.search.value,
      searchResults: response.data.results
    });
  };

  render() {
    return (
      <form>
        <input
          key="search"
          placeholder="Search for..."
          ref={input => (this.search = input)}
          onChange={this.handleInputChange}
        />
        <br />
        <input
          key="includeClasses"
          type="checkbox"
          name="includeClasses"
          value="true"
          ref={input => (this.includeClassesInput = input)}
          onChange={this.handleInputChange}
          defaultChecked
        />
        Classes |
        <input
          key="includeExtensionMethods"
          type="checkbox"
          name="includeExtensionMethods"
          ref={input => (this.includeExtensionMethodsInput = input)}
          onChange={this.handleInputChange}
          value="true"
        />
        ExtensionMethods |
        <input
          key="includeGlobalMethods"
          type="checkbox"
          name="includeGlobalMethods"
          ref={input => (this.includeGlobalMethodsInput = input)}
          onChange={this.handleInputChange}
          value="true"
        />
        GlobalMethods
        <p>{this.state.query}</p>
        <ResultList searchResults={this.state.searchResults} />
      </form>
    );
  }
}

export default ArtifactSearch;
