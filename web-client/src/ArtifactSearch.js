import React, { Component } from 'react'
import axios from 'axios'

class SearchResult extends Component {
    render() {
        const item = this.props.item
        return (
            <div>
                <p>{item.name} ({item.score}) @ {item.groupId}:{item.artifactId}:{item.version} </p>
            </div>
        );
    }
}

function ResultList(props) {
    const listItems = props.searchResults.map((item) =>
      <li key={item.id}>
        <SearchResult item={item}/>
      </li>
    );
    return (
      <ul>{listItems}</ul>
    );
}

class ArtifactSearch extends Component {
    state = {
      query: '',
      searchResults: []
    }

    handleInputChange = async event => {
        event.preventDefault();
      
        // Promise is resolved and value is inside of the response const.
        // https://jsonplaceholder.typicode.com/users
        // http://0.0.0.0:8080/searchArtifact
        const response = await axios.get(`/searchArtifact`, {
            params : {
                query: this.search.value
            },
            headers: { 'Content-Type': 'application/json' }
        });
        console.log(response);
        console.log(response.data);
        this.setState( {
            query: this.search.value,
            searchResults: response.data
        })
      
        
      };
   
    render() {
      return (
        <form>
          <input
            placeholder="Search for..."
            ref={input => this.search = input}
            onChange={this.handleInputChange}
          />
          <p>{this.state.query}</p>
          <ResultList searchResults={this.state.searchResults}/>
        </form>
      )
    }
   }
   
   export default ArtifactSearch